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
- Solar Term ranges with Zi-day boundaries and a continuous absolute index across years
- Zi Hour (子时) handling
- Li Chun (立春) year boundary
- Leap month (闰月) support
- Five Elements (五行)
- Yin / Yang (阴阳)
- Four Seasons (四季)
- Earth transition periods (土旺十八日)
- Shanghai timezone normalization
- Injectable `Clock` for the current time (testable "now")
- Thread-safe caches for Solar Terms, Solar Term ranges and Earth transitions

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
- birth charts on local solar time (真太阳时), see below

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
│   ├── SolarTermRangeCache
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
- Solar Term range cache (including a check of all 24 moments of 2014 against Hong Kong Observatory data)
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
    ShanghaiTimeResolver.now().toLocalDateTime()
)

println(lunar.yearStem)
println(lunar.monthStem)
println(lunar.dayStem)
println(lunar.hourStem)

println(lunar.solarTerm)
println(lunar.seasonQiElement)
```

### Birth chart on true solar time

```kotlin
// Born in Berlin (13.4° E) at 23:20 local time
val birth = ZonedDateTime.of(1990, 6, 15, 23, 20, 0, 0, ZoneId.of("Europe/Berlin"))

val chart = LunarDateConverter.fromBirth(birth, longitude = 13.4)
// Year / month: real instant of birth. Day / hour: true solar time (≈ 22:13 → 亥, 15 June).
// On Beijing time the same moment would be 05:20 on 16 June (卯).

val calibrated = LunarDateConverter.fromBirth(birth, 13.4, hourBranchShift = 1) // 子 of 16 June
```

### Solar Term ranges

```kotlin
val current = SolarTermRangeCache.resolveActiveSolarTerm(Instant.now())

println(current.term)                      // e.g. SolarTerm.LI_CHUN
println(current.ziStart..current.ziEnd)    // Zi days of the term, inclusive

// Navigate across years by absolute index
val previous = SolarTermRangeCache.findByAbsoluteIndex(current.absoluteIndex - 1)
```

---

## Current Status

Current implementation includes:

- ✔ Gregorian → Lunar conversion
- ✔ Solar Terms
- ✔ Solar Term ranges (Zi days, absolute index)
- ✔ GanZhi
- ✔ Leap Months
- ✔ Zi Hour
- ✔ Birth charts on local mean / true solar time
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