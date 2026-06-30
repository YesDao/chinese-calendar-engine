# Chinese Calendar Engine

A Kotlin library for Chinese calendrical calculations based on traditional Chinese calendar rules.

The library provides conversion from the Gregorian calendar to a structured Chinese calendar model including Lunar Date, GanZhi (干支), Solar Terms (节气), Heavenly Stems (天干), Earthly Branches (地支), Wu Xing (五行) and related calendrical concepts.

---

## Features

- Gregorian → Lunar calendar conversion
- 60 GanZhi cycle
- Heavenly Stems (天干)
- Earthly Branches (地支)
- 24 Solar Terms (节气)
- Zi Hour (子时) handling
- Li Chun (立春) year boundary
- Leap month (闰月) support
- Five Elements (五行)
- Yin / Yang (阴阳)
- Four Seasons (四季)
- Earth transition periods (土旺十八日)
- Shanghai timezone normalization
- Thread-safe caches for Solar Terms and Earth transitions

---

## Design Goals

Unlike many calendar wrappers, this project exposes a strongly typed domain model instead of raw strings.

Example:

```kotlin
val lunar = LunarDateConverter.fromGregorian(
    LocalDateTime.of(2024, 2, 10, 8, 0)
)

println(lunar.yearStem)
println(lunar.dayBranch)
println(lunar.solarTerm)
println(lunar.seasonQiElement)
```

instead of

```text
"甲"
"子"
"立春"
```

the API returns

```kotlin
HeavenlyStem.JIA
EarthlyBranch.ZI
SolarTerm.LI_CHUN
FiveElement.WOOD
```

---

## Time Handling

All calculations are normalized to **Asia/Shanghai**, which is the reference timezone used by the traditional Chinese calendar.

The library correctly implements:

- Zi Hour (子时)
- day transition after 23:00
- Li Chun year transition
- Solar Term based month calculations

---

## Project Structure

```
src
├── model
│   ├── LunarDate
│   ├── HeavenlyStem
│   ├── EarthlyBranch
│   ├── GanZhi
│   ├── SolarTerm
│   ├── FiveElement
│   ├── FiveColor
│   ├── FourSeason
│   ├── Organ
│   └── YinYang
│
├── converter
│   └── LunarDateConverter
│
├── time
│   └── ShanghaiTimeResolver
│
├── cache
│   ├── JieQiCache
│   └── EarthTransitionCache
```

---

## Testing

The project contains unit tests covering both implementation and calendrical invariants.

### Converter

- Gregorian conversion
- Leap month handling
- Lunar New Year
- Timezone normalization
- Forecast month logic

### Calendar Invariants

- Li Chun year transition
- GanZhi cycle
- 24 Solar Terms
- Leap month boundaries
- New Moon month transitions
- Twelve traditional Chinese hours
- Jia Zi cycle

### Infrastructure

- Shanghai time normalization
- JieQi cache
- Earth transition cache

---

## Domain Model

The central object returned by the library is:

```kotlin
LunarDate
```

which contains

- Four Pillars (BaZi)
- Lunar month/day
- Leap month information
- Solar Term
- GanZhi indices
- Forecast month pillars
- Tai Sui month pillars
- Season
- Five Element of seasonal Qi

---

## Example

```kotlin
val lunar = LunarDateConverter.fromGregorian(
    LocalDateTime.now()
)

println(lunar.yearStem)
println(lunar.monthStem)
println(lunar.dayStem)
println(lunar.hourStem)

println(lunar.solarTerm)
println(lunar.seasonQiElement)
```

---

## Current Status

Current implementation includes:

- ✔ Gregorian → Lunar conversion
- ✔ Solar Terms
- ✔ GanZhi
- ✔ Leap Months
- ✔ Zi Hour
- ✔ Li Chun year boundary
- ✔ Forecast month calculation
- ✔ Tai Sui month calculation
- ✔ Thread-safe caches
- ✔ Unit tests

---

## Roadmap

Future work includes

- Moon Mansions (二十八宿)
- Nine Stars (九星)
- Qi Men Dun Jia support
- Xuan Kong calculations
- Additional astronomical utilities

---

## License

MIT