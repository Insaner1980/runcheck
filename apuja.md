val batteryManager =
    context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

val currentMicroamps =
    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

val currentMilliamps = currentMicroamps / 1000


Android 14+ (sinulla toimii)

Android 14 toi oikeasti hyödyllisiä uusia propertyjä.

BATTERY_PROPERTY_CHARGING_CYCLE_COUNT
BATTERY_PROPERTY_STATE_OF_HEALTH

Kotlin:

val cycles = batteryManager.getIntProperty(
    BatteryManager.BATTERY_PROPERTY_CHARGING_CYCLE_COUNT
)

val health = batteryManager.getIntProperty(
    BatteryManager.BATTERY_PROPERTY_STATE_OF_HEALTH
)

OEM:t blokkaavat sensoridataa

Esimerkiksi:

Xiaomi

Huawei

Oppo

Samsung

Usein:

sysfs polut poistettu

thermal zones piilotettu

battery stats rajoitettu

1. Coulomb Counter (tarkempi kuin prosentit)

Tämä on yksi Androidin alikäytetyimmistä sensoreista.

API:

BATTERY_PROPERTY_CHARGE_COUNTER

Se antaa akun todellisen varauksen µAh.

Esimerkki:

4200000 µAh

→

4200 mAh
Miksi tämä on hyvä

Battery percentage on:

pyöristetty

mutta charge counter on:

todellinen kapasiteetti
Näin saat todellisen kulutuksen
Δcharge / Δtime

Esimerkki:

4200 mAh → 4198 mAh
aika: 60 sekuntia

Kulutus:

2 mAh / 60 s

→

120 mA
Kotlin
val batteryManager =
    context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

val chargeMicroAh =
    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

val chargeMah = chargeMicroAh / 1000

Tämä toimii monesti luotettavammin kuin CURRENT_NOW.

2. Current + Voltage = todellinen teho

Monet sovellukset näyttävät vain mA.

Mutta oikeasti kiinnostava arvo on:

power (W)

kaava:

P = V × I
Esimerkki
current = -1500 mA
voltage = 3.9 V
power = 5.85 W

Tämä kertoo kuinka paljon energiaa laite kuluttaa oikeasti.

Voltage API
BATTERY_PROPERTY_VOLTAGE

tai broadcast:

BatteryManager.EXTRA_VOLTAGE
Kotlin
val current =
    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

val voltage =
    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_VOLTAGE)

val currentA = current / 1_000_000.0
val voltageV = voltage / 1000.0

val powerW = currentA * voltageV

UI:ssa tämä on todella näyttävä:

Battery Power
5.8 W
3. Moving Average Filtering (poistaa sensorin “kohinan”)

CURRENT_NOW voi heilahdella:

1200 mA
-300 mA
2000 mA
-500 mA

koska CPU tekee piikkejä.

Ratkaisu:

moving average

esim viimeiset 10 mittausta.

Algoritmi
avg = sum(last_n_values) / n
Kotlin
class MovingAverage(private val size: Int) {

    private val values = ArrayDeque<Int>()

    fun add(value: Int): Double {
        if (values.size == size) {
            values.removeFirst()
        }

        values.addLast(value)

        return values.average()
    }
}

UI näyttää silloin:

Instant current
1450 mA

Smoothed current
1320 mA

Se näyttää paljon ammattimaisemmalta.

Bonus: cycle count (Pixelissä toimii)

Android 14+

BATTERY_PROPERTY_CHARGING_CYCLE_COUNT

UI:

Battery cycles
127

Tämä on todella suosittu feature.

Jos yhdistät nämä kolme

1️⃣ Charge counter delta
2️⃣ Current + voltage power
3️⃣ moving average smoothing

saat sovelluksen joka on teknisesti parempi kuin suurin osa Play Storen battery appsista.

Monet näyttävät vain:

Battery usage estimate

sinä näytät:

Live power draw

Se on aivan eri taso. ⚡

Hyvä. Kaivetaan esiin **kolme Android-sensoria**, joita lähes kukaan battery/device-monitor-sovellus ei käytä kunnolla. Kun nämä tuodaan UI:hin, sovellus näyttää heti **insinööritason työkalulta eikä geneeriseltä health-appilta**. ⚙️📱

Testilaitteesi **Google Pixel 9** ja uusin **Android** tukevat näitä yleensä hyvin.

---

# 1. Thermal Zones (CPU / modem / battery lämpötilat)

Androidin kernel exporttaa **kaikki lämpösensorit** tänne:

```
/sys/class/thermal/
```

Esim:

```
thermal_zone0
thermal_zone1
thermal_zone2
```

Jokaisella on:

```
type
temp
```

### Esimerkki

```
type: cpu-thermal
temp: 48000
```

→

```
48°C
```

### Miksi tämä on hyvä

Useimmat sovellukset näyttävät vain:

```
Battery temperature
```

Mutta Pixelissä voi nähdä:

```
CPU temp
GPU temp
Modem temp
Battery temp
Skin temp
```

UI idea:

```
Device Thermals

CPU        52°C
GPU        49°C
Battery    41°C
Modem      55°C
Skin       38°C
```

Tämä näyttää **oikealta device-diagnostiikalta**.

---

# 2. GPU Load (harvinainen mutta erittäin näyttävä)

GPU-kuormitus löytyy usein kernelistä:

```
/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage
```

Esimerkki:

```
37
```

→

```
GPU load 37%
```

UI:

```
GPU activity
37%
```

Kun käyttäjä avaa pelin, arvo nousee heti.

Se tekee sovelluksesta:

```
live performance monitor
```

---

# 3. CPU Frequency per Core

Tämä on **todella visuaalinen feature**.

Polku:

```
/sys/devices/system/cpu/
```

Esim:

```
cpu0/cpufreq/scaling_cur_freq
cpu1/cpufreq/scaling_cur_freq
cpu2/cpufreq/scaling_cur_freq
```

Arvo:

```
1800000
```

→

```
1.8 GHz
```

UI-idea:

```
CPU cores

Core 0   1.8 GHz
Core 1   2.0 GHz
Core 2   1.2 GHz
Core 3   900 MHz
```

Tai Jetpack Compose graph:

```
█ █ █ █ █ █
```

live-päivittyvä.

---

# 4. Modem Power State (erittäin niche)

Pixel-laitteissa voi lukea:

```
/sys/class/net/rmnet_data0/statistics/
```

tai modem-power-state debug.

UI:

```
Cellular radio

Idle
Active
Data transfer
```

Kun käyttäjä lataa dataa:

```
Radio power spike
```

Tämä selittää **akun kulutusta paremmin kuin perinteiset battery-appsit**.

---

# 5. SurfaceFlinger FPS (ruudun todellinen FPS)

Android exporttaa joskus:

```
dumpsys SurfaceFlinger
```

josta saa:

```
frame latency
```

UI:

```
Screen refresh

120 FPS
```

Pixelissä tämä on todella näyttävä.

---

# Jos yhdistät nämä samaan sovellukseen

Saat **device observability toolin**, ei vain battery appia.

Esim UI:

```
DEVICE CORE

Power draw
5.4 W

CPU load
38 %

GPU load
27 %

Thermals
CPU 51°C
Battery 40°C

CPU freq
2.4 GHz
```

Tämä näyttää enemmän:

```
developer diagnostic tool
```

kuin

```
battery health app
```

---

Jos haluat sovelluksen joka toimii **Samsungissa, Xiaomissa, OnePlusissa, Pixelissä, Motorolassa jne**, pitää käyttää vain sensoreita jotka ovat **Android API:ssa tai lähes universaaleja kernel-exportteja**.

Alla realistinen tilanne.

---

# 1. BATTERY_PROPERTY_CURRENT_NOW

✔ toimii monilla merkeillä
❗ mutta ei kaikilla

API:

```
BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
```

Saat:

```
µA (microamp)
```

Esim

```
-850000
```

→

```
-850 mA
```

### Tuki käytännössä

Hyvä tuki:

* Samsung
* Xiaomi
* OnePlus
* Google
* Motorola

Huono tai puuttuu joskus:

* Oppo
* Vivo
* halvat kiinalaiset OEM:t

### Suositus

```
try CURRENT_NOW
fallback → charge counter delta
```

---

# 2. BATTERY_PROPERTY_CHARGE_COUNTER

✔ erittäin hyvä tuki

API:

```
BATTERY_PROPERTY_CHARGE_COUNTER
```

Tämä toimii todella monessa puhelimessa koska se tulee **fuel gauge chipiltä**.

Tuki:

* Samsung ✔
* Xiaomi ✔
* Pixel ✔
* Motorola ✔
* OnePlus ✔
* Oppo ✔ (usein)

Harvoin puuttuu.

### Tämä on paras universaali mittaus

voit laskea:

```
Δcharge / Δtime = current
```

---

# 3. Voltage

✔ käytännössä universaali

Broadcast:

```
BatteryManager.EXTRA_VOLTAGE
```

Tuki:

≈ kaikki Android-laitteet.

---

# 4. Thermal sensors

⚠ vaihtelee paljon

polku:

```
/sys/class/thermal/
```

Ongelma:

* sensorin nimet eri
* osa piilotettu
* osa vaatii rootin

Esimerkki:

Samsung

```
cpu-thermal
battery
gpu
```

Xiaomi

```
soc
battery
quiet-therm
```

Toimii mutta **vaatii heuristiikkaa**.

---

# 5. CPU frequency

✔ toimii lähes kaikissa

polku:

```
/sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq
```

Tuki:

≈ 90% Android-laitteista.

Tämä on hyvä feature.

---

# 6. GPU load

❌ EI universaali

polku:

```
/sys/class/kgsl/
```

vain:

* Qualcomm GPU

ei toimi:

* Mali GPU
* Immortalis GPU

Eli:

Samsung Exynos → ei.

---

# 7. Cycle count

⚠ Android 14+

```
BATTERY_PROPERTY_CHARGING_CYCLE_COUNT
```

Toimii:

* Pixel
* Samsung uudemmat

mutta ei kaikissa.

---

# Jos teet **maksimi-yhteensopivan appin**

Suosittelen tätä sensoripinoa:

### 1️⃣ universaali battery layer

```
charge counter
current_now (optional)
voltage
temperature
battery %
```

### 2️⃣ performance layer

```
CPU frequency per core
CPU load (proc/stat)
RAM usage
```

### 3️⃣ optional layer

```
thermal zones
GPU load
cycle count
```

Jos sensori puuttuu → UI piilottaa sen.

---

# Yksi erittäin tärkeä vinkki

Älä koskaan tee tätä:

```
feature not supported
```

vaan:

```
sensor unavailable on this device
```

Se näyttää **ammattilaiselta** eikä bugilta.

---



1. Delta-Charge Current (tarkin universaali akun virran mittaus)

Useimmat sovellukset yrittävät lukea suoraan:

BATTERY_PROPERTY_CURRENT_NOW

Ongelma: monessa laitteessa arvo puuttuu tai on epävakaa.

Parempi tapa on käyttää:

BATTERY_PROPERTY_CHARGE_COUNTER

ja laskea virta itse.

Idea
current = Δcharge / Δtime
Esimerkki
t0 = 4200 mAh
t1 = 4198 mAh
aika = 60 s

kulutus:

2 mAh / 60 s

→

120 mA
Miksi tämä on hyvä

toimii Samsungissa

toimii Xiaomissa

toimii Pixelissä

toimii Motorolassa

toimii OnePlussassa

koska lähes kaikissa on fuel-gauge chip joka pitää charge counteria.

2. Procfs CPU Load (oikea CPU-kuorma)

Monet sovellukset käyttävät epätarkkoja metodeja.

Luotettavin tapa on lukea:

/proc/stat
Esimerkki
cpu  12234 332 9231 998812 1123 0 221 0

Algoritmi:

load = (Δactive / Δtotal)
Miksi tämä on hyvä

toimii kaikissa Linux-pohjaisissa Android-laitteissa

ei riippuvainen valmistajasta

erittäin kevyt lukea

UI voi näyttää:

CPU usage
37%

tai jopa per core.

3. Screen Power Estimation (iso akun kuluttaja)

Näyttö on usein suurin energiankuluttaja, mutta useimmat sovellukset eivät arvioi sitä.

Voit yhdistää:

brightness
refresh rate
screen on time

API:t:

Settings.System.SCREEN_BRIGHTNESS
Display.getRefreshRate()
PowerManager.isInteractive()
Yksinkertainen arvio
screen_power ≈ brightness % × panel max power

Esim:

brightness 70%
panel max 3.5 W

→

≈ 2.45 W

UI:

Display power
2.4 W

Se selittää käyttäjälle miksi akku tyhjenee.

Jos yhdistät nämä kolme

Saat hyvin tarkasti:

device power draw

Esim UI:

DEVICE POWER

Total draw
5.8 W

CPU
1.6 W

Display
2.4 W

Radio
0.8 W

Other
1.0 W

Tämä tekee sovelluksesta enemmän device diagnostics toolin kuin perinteisen battery appin, joka toimii hyvin eri Android-versioissa kuten **Android.

💡 Yksi tärkeä UX-vinkki loppuun:

Näytä aina confidence level mittaukselle.

Esim:

Current draw
1.4 A
(measured)

vs

Display power
2.2 W
(estimated)
