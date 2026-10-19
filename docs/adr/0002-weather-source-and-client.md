# ADR 0002: Weather source and HTTP client (MET Norway Locationforecast 2.0 "complete", OkHttp with HTTP cache)

- Status: Accepted
- Date: 2026-10-19
- Run: `20261019-dog-walk-buddy-mvp`

## Context
The walk check needs these values for the next 12 hourly periods at the user's approximate location:
- air temperature
- 1-hour precipitation
- mean wind
- wind gust
- cloud cover

The source must be free, keyless and allowed for commercial use (spec, Dependencies). There must be no backend or proxy.

MET Norway's terms require three things:
- an identifying User-Agent (generic agents are throttled or blocked)
- respect for `Expires` and `Last-Modified`, with conditional requests
- no more than 4 decimals in coordinates

Its data is CC BY 4.0 / NLOD, and the attribution "MET Norway" is required. Responses are untrusted input (security standards).

The Locationforecast data model page lists `wind_speed_of_gust` in the **complete** product, not in **compact**. Spec Assumption 1 requires the walking skeleton to confirm that gust and cloud cover are present at hourly resolution for a UK point.

## Options considered
1. **MET Norway Locationforecast 2.0 `complete`, called directly with OkHttp and its disk cache.**
   - Pros:
     - Has gust, cloud, temperature, precipitation and wind.
     - Free, keyless and commercial-use.
     - OkHttp's RFC 7234 cache serves fresh responses without a request and sends `If-Modified-Since` when stale, which is what MET's terms ask.
   - Cons:
     - A larger payload than `compact` (about 100-200 kB uncompressed, gzip on the wire).
     - The MET terms "should use a proxy" risk (spec, Medium).
2. **Locationforecast `compact`.**
   - Pros: smaller.
   - Cons: no gust, so every hour would be "never Go" (Assumption 6). Rejected.
3. **Open-Meteo, Met Office DataHub, others.** Rejected in the spec: non-commercial or keyed.
4. **`HttpURLConnection` without a library.**
   - Pros: no dependency.
   - Cons: no HTTP cache semantics, so caching and conditional-request logic would have to be written by hand, and there would be no stub server for tests.

## Decision
Option 1.

- **Endpoint:** `https://api.met.no/weatherapi/locationforecast/2.0/complete?lat=<lat>&lon=<lon>`.
  - The base URL is a constructor parameter, so tests point it at MockWebServer.
  - The production base URL is a named constant.
- **Coordinates:** rounded to 2 decimals with `BigDecimal` `HALF_UP` and formatted with `Locale.ROOT`. For example, 51.507351, −0.127758 becomes `lat=51.51&lon=-0.13` (AC23). There is no `altitude` parameter.
- **User-Agent:** `DogWalkBuddy/<versionName> (+https://github.com/greavemr-svg/dog-walk-buddy)`.
  - It identifies the app with a contact URL, as MET requires, without a personal email.
  - It is the only identifying header. There is no device, advertising or user ID (AC23).
  - The owner may change the contact URL. It is a constant in `WeatherConfig`.
- **Caching:** an OkHttp `Cache` of 2 MiB in `cacheDir/http-weather`, honouring `Expires` and `Last-Modified`.
  - It is a separate cache from the map tile cache.
  - "Delete all my data" evicts it (AC47).
- **Timeouts:** `callTimeout(10 s)`, as the spec NFR requires.
- **Status handling:**
  - Only 200 (or 304 resolved by the cache) is success. 203 (deprecated product) is also success, but logged without personal data.
  - 4xx, 5xx, 429 and timeouts are a failed check. AC22 handles these by showing the cache (under 3 h old) or "Can't check right now".
- **HTTPS only:** an application interceptor rejects any non-`https` URL before it is sent. This is defence in depth on top of the network security config. The interceptor allows `http` only when the test-only `allowInsecureForTests` flag is set, which no production code path sets. AC23's test uses an HTTPS MockWebServer through `okhttp-tls`, so the flag is not needed there either.
- **Parsing and validation:** `kotlinx.serialization` with `ignoreUnknownKeys = true` and every field nullable.
  - Response bodies over 2 MiB are rejected.
  - Each period is validated into a domain `HourlyConditions(startUtc, temperatureC?, precipitationMmPerHour?, windMeanMph?, windGustMph?, cloudCoverPercent?)`. Accepted ranges:

    | Field | Accepted range |
    |---|---|
    | Temperature | −60 to 60 °C |
    | Precipitation | 0 to 500 mm/h |
    | Mean wind | 0 to 100 m/s |
    | Gust | 0 to 150 m/s |
    | Cloud cover | 0 to 100 % |

  - A value outside its range, non-finite or the wrong type becomes `null` ("missing").
  - A malformed document (bad JSON, no `properties.timeseries`, unparseable times) is a **failed check**. It never crashes and never gives Go.
  - Wind is converted from m/s to mph with the constant 2.236936.
  - Precipitation comes from `data.next_1_hours.details.precipitation_amount` (mm over 1 h, so mm/h). Temperature, wind, gust and cloud come from `data.instant.details`.
- **Hourly periods:** the 12 consecutive hourly periods starting with the one that contains "now" (from the injected `Clock`).
  - If the response is not hourly for those 12 periods, those hours are treated as missing.
  - If the current hour is entirely missing, AC15 applies ("Can't check right now").
- **Attribution:** "Weather data: MET Norway (CC BY 4.0)" is shown on the check screen (AC27). The app name and branding never use "Yr" or imply endorsement.

### Walking-skeleton verification (Assumption 1)
Task 1 runs this command once, from a developer machine:

```
curl -sS -H "User-Agent: DogWalkBuddy/0.1.0 (+https://github.com/greavemr-svg/dog-walk-buddy)" "https://api.met.no/weatherapi/locationforecast/2.0/complete?lat=51.51&lon=-0.13"
```

It commits the response as `app/src/test/resources/met/complete_london_51.51_-0.13.json`. Forecast data is public and not personal data.

It adds the unit test `Assumption1_met_complete_has_hourly_gust_and_cloud_for_uk_point`. The test asserts that the first 12 timeseries entries are 1 hour apart and each has `air_temperature`, `wind_speed`, `wind_speed_of_gust`, `cloud_area_fraction` and `next_1_hours.precipitation_amount`.

The PR records the capture time and the HTTP status. If any of those fields is absent, task 1 stops and reports BLOCKED for an owner decision, as the spec requires.

## Consequences
- The app depends on MET's tolerance of direct mobile calls.
  - Coarse rounding (2 decimals) means users in the same ~1 km cell share cacheable URLs.
  - Checks only run on demand (AC24), so volume stays low.
  - If traffic grows, the owner must revisit "no backend". That is recorded as a risk in the spec and the design.
- Swapping the provider later touches only `data/weather/*`. The domain `HourlyConditions` is provider-neutral.

## For new dependencies
- Licence: OkHttp Apache-2.0. kotlinx.serialization Apache-2.0. Data: CC BY 4.0 / NLOD.
- Maintenance: see ADR 0001.
- Known vulnerabilities: none known for OkHttp 5.1.0 at design time.
- Size / performance impact: OkHttp plus Okio add about 1 MB before R8.
