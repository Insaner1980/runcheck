# Info Tooltips — Selitystekstit

Runcheck näyttää paljon teknisiä mittareita joita tavallinen käyttäjä ei välttämättä ymmärrä. Selitystekstit auttavat käyttäjää ymmärtämään mitä luvut tarkoittavat ja miksi ne ovat tärkeitä.

---

## Toteutus

### Komponentit

**`InfoButton`** — pieni ⓘ-ikoni joka näytetään labelin tai otsikon vieressä.
- Ikoni: `Icons.Outlined.Info`, koko 18dp
- Väri: `onSurfaceVariant`
- Napautus avaa bottom sheetin
- Touch target min 48dp

**`InfoBottomSheet`** — Modal bottom sheet joka näyttää selityksen.
- Otsikko: `titleMedium`, `onSurface`
- Selitysteksti: `bodyMedium`, `onSurfaceVariant`
- Sulkeminen: drag down tai tausta-napautus
- Ei erillistä X-nappia — M3 bottom sheet standard behavior riittää

### Sijoittelu

ⓘ-ikoni sijoitetaan:
- `MetricPill`: labelin perään (valinnainen `onInfoClick` parametri)
- `CardSectionTitle`: otsikon perään (valinnainen `onInfoClick` parametri)
- Erillinen `Row` jossa label + `InfoButton` — käytettäessä muissa yhteyksissä

### Tiedostorakenne

- `ui/components/InfoButton.kt` — ⓘ-ikoni composable
- `ui/components/InfoBottomSheet.kt` — bottom sheet composable
- Selitystekstit `strings.xml` / `strings-fi.xml` — lokalisoitu

---

## Selitettävät mittarit

### Battery-näkymä

| Mittari | Sijainti | Selitys (EN) |
|---------|----------|-------------|
| Current (mA) | Charging/Battery Current -osion "Current" label | Electric current measures the flow of energy to or from your battery. Positive values mean charging, negative values mean discharging. Higher discharge current means faster battery drain and more heat. |
| Battery Health | Battery Info -osion Health-rivi | Battery health indicates how much of the original capacity your battery retains. Over time and charge cycles, capacity naturally decreases. Above 80% is considered good. |
| Cycle Count | Battery Info -osion Cycle Count -rivi | A charge cycle is one full discharge from 100% to 0%. Partial charges count proportionally — two 50% charges equal one cycle. Most batteries maintain good health for 500–800 cycles. |
| Confidence Badge | Confidence badge -komponentin vieressä | "Accurate" means the reading comes directly from a reliable hardware sensor. "Estimated" means the value is calculated or the sensor may not be fully reliable on this device. |
| Charging Speed (%/h) | Charging session -osion Recent Speed / Avg Speed | Charging speed shows how fast the battery level increases per hour. Speed varies based on charger wattage, battery temperature, and current charge level — phones typically slow down above 80%. |
| Power (W) | Charging/Battery Current -osion power-arvo | Power in watts shows the total energy flow rate. It's calculated from current (mA) and voltage (mV). Higher wattage during charging means faster charging but also more heat. |

### Thermal-näkymä

| Mittari | Sijainti | Selitys (EN) |
|---------|----------|-------------|
| Thermal Headroom | ThermalMetrics-kortin Headroom-pilli | Thermal headroom shows how much thermal capacity your device has left before it needs to throttle performance. Higher percentage means more room. Below 30% may trigger throttling. |
| Thermal Throttling | ThermalMetrics-kortin Throttling-pilli | When your device gets too hot, it reduces processor speed to cool down. This is called thermal throttling. It protects the hardware but temporarily slows down your phone. |
| CPU Temperature | ThermalMetrics-kortin CPU Temp -pilli | The processor temperature. Normal range is 25–45°C. Gaming or heavy apps can push it higher. Sustained temperatures above 50°C may trigger throttling. |

### Network-näkymä

| Mittari | Sijainti | Selitys (EN) |
|---------|----------|-------------|
| Signal Strength (dBm) | Hero-osion dBm-arvo | Signal strength in dBm (decibel-milliwatts). Values are negative — closer to 0 is stronger. WiFi: -30 excellent, -67 good, -70 fair, -80 poor. Cellular varies by technology. |
| Latency | Hero-osion Latency-pilli | Latency (ping) measures the round-trip time for data to travel to a server and back, in milliseconds. Lower is better: under 20ms is excellent, under 50ms is good for most uses, over 100ms may feel laggy. |
| Link Speed | Connection Details -osion Link Speed -rivi | The maximum data transfer rate your WiFi connection supports, in megabits per second. Actual throughput is typically lower due to interference, distance, and network congestion. |
| Est. Bandwidth | Connection Details -osion Est. Bandwidth -rivit | Estimated bandwidth is the system's prediction of available throughput based on recent network performance. It's an approximation — run a speed test for more accurate results. |

### Storage-näkymä

Ei selitystekstejä tarvita — tallennustila on yleisesti ymmärretty konsepti.

---

## Lokalisointi

Kaikki selitystekstit lisätään `strings.xml`:ään avaimella `info_[category]_[metric]`:

```
info_battery_current
info_battery_health
info_battery_cycle_count
info_battery_confidence
info_battery_charging_speed
info_battery_power
info_thermal_headroom
info_thermal_throttling
info_thermal_cpu_temp
info_network_signal_dbm
info_network_latency
info_network_link_speed
info_network_est_bandwidth
```

Suomenkieliset käännökset `values-fi/strings.xml`:ään.

---

## Prioriteettijärjestys

Toteutusjärjestys — ensin ne joita käyttäjä todennäköisimmin ei ymmärrä:

1. **Confidence Badge** — "Accurate"/"Estimated" on app-spesifinen konsepti
2. **Thermal Headroom** — tekninen termi jota harva tuntee
3. **Current (mA)** — positiivinen/negatiivinen voi hämmentää
4. **Signal Strength (dBm)** — negatiiviset luvut hämmentävät
5. **Battery Health** — yleisemmin tunnettu, mutta % vaihtelee
6. **Cycle Count** — melko tunnettu mutta laskutapa ei
7. **Loput** — power, charging speed, throttling, latency, link speed, bandwidth
