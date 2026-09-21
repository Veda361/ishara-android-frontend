# Ishaara Design System — Human-Centered Transit Foundation

**Document Version**: 2.0.0  
**Status**: ACTIVE / PRODUCTION DESIGN SYSTEM  
**Platform**: Android (Jetpack Compose & Material 3)  
**Target Roles**: User / Student & Driver / Conductor  

---

## 1. Core Philosophy: Human-Centered Mobility

Ishaara is a **real-world public transportation and campus mobility product**.

The interface is built around one uncompromising design rule:
> **"Simple enough for a first-time user.  
> Fast enough for a driver.  
> Clear enough for a student.  
> Professional enough for a mobility company."**

### Visual Quality Bar
- **Clarity over Novelty**: If a decorative element competes with an action, it is eliminated.
- **Natural Human Language**: Technical acronyms and cyber/hacker-style brackets belong in debug logs, never in the UI.
- **One Screen = One Primary Action**: The primary action (e.g. "Request ride", "Start trip", "Go online") is visually obvious and placed in the lower thumb zone (54dp high).
- **Indian Transit Context**: Built for students checking bus arrival times while walking in sunlight, and drivers glancing at their phones while stopped at a transit bay.

---

## 2. Human-Centered UX & Typography

### Primary Font Family
- **Font Family**: Clean Modern Sans-Serif (`FontFamily.Default`, resolving to Inter / Roboto / Google Sans on Android).
- **Rationale**: 
  - Maximum legibility at small sizes and high ambient glare.
  - Zero cognitive overhead compared to decorative, condensed, or monospace fonts.
  - Full support for Android system dynamic font scaling without line truncation.

### Restrained Typography Scale
| Style | Size / Line Height | Weight | Usage |
| :--- | :--- | :--- | :--- |
| `displayLarge` | `32sp` / `40sp` | Bold | Hero statements (e.g., "Getting to college") |
| `displaySmall` | `28sp` / `36sp` | Bold | Secondary hero headers |
| `headlineLarge`| `24sp` / `32sp` | SemiBold | Screen titles (e.g., "Available buses", "Active trip") |
| `headlineMedium`| `20sp` / `28sp` | SemiBold | Route titles (e.g., "Jhansi → Orchha") |
| `headlineSmall`| `18sp` / `24sp` | SemiBold | Card titles and section headers |
| `titleLarge` | `16sp` / `24sp` | SemiBold | Component headers, dialog titles |
| `titleMedium` | `15sp` / `22sp` | Medium | Prominent list item titles |
| `titleSmall` | `14sp` / `20sp` | Medium | Field labels, input descriptors |
| `bodyLarge` | `16sp` / `24sp` | Regular | Primary body copy, search inputs |
| `bodyMedium` | `14sp` / `20sp` | Regular | Secondary descriptions, ETA and fare info |
| `bodySmall` | `13sp` / `18sp` | Regular | Captions, timestamps, helper notes |
| `labelLarge` | `15sp` / `20sp` | SemiBold | Buttons (Large & Medium) |
| `labelMedium` | `13sp` / `18sp` | Medium | Small buttons, chip titles |
| `labelSmall` | `12sp` / `16sp` | Medium | Seat counts, status tags |

### Font Weights
Only four standard weights are utilized across the entire product:
- `Regular` (400)
- `Medium` (500)
- `SemiBold` (600)
- `Bold` (700)

*Excluded*: Thin, ExtraLight, Black, and condensed decorative weights.

### Sentence-Case Rules (No Forced All-Caps)
- **BAD**: `REQUEST RIDE`, `START TRIP`, `SEARCH LOCATION`, `CONFIRM BOARDING`
- **GOOD**: `Request ride`, `Start trip`, `Search location`, `Confirm boarding`
- All button labels, input headers, route summaries, and system messages use **natural sentence case**.
- Uppercase is restricted strictly to very short abbreviations where conventional (e.g., `AC`, `ETA`, `SOS`, `UPI`).

---

## 3. UX Writing & Natural Language Rules

Every label, message, and prompt must sound like a real human:

| Instead of (Technical / Cyber) | Use (Human-Centered Mobility) |
| :--- | :--- |
| `[ SYSTEM LIVE ]` | `Live` |
| `[ ETA 06 MIN ]` | `6 min away` |
| `RIDE REQUEST PENDING` | `Waiting for driver` |
| `DRIVER ARRIVAL STATE` | `Driver arrived` |
| `TRIP_EXECUTION_ACTIVE` | `Trip started` |
| `SELECT_ORIGIN_COORDINATE` | `Choose pickup point` |
| `DESTINATION INPUT` | `Where are you going?` |
| `SEATS_REMAINING: 12` | `12 seats available` |
| `PAYMENT_GATEWAY_SUCCESS` | `Payment complete` |
| `HTTP 409 CONFLICT` | `Ride request could not be completed. Please try again.` |

---

## 4. Color System

A restrained, high-contrast palette prioritizing readability over decoration:

### Semantic Tokens
- **Background**: Crisp White (`#FFFFFF` in light mode, `#0D0F12` in dark mode).
- **Surface**: Off-White (`#F9FAFB` in light, `#14171D` in dark).
- **Surface Elevated**: Clean White Card (`#FFFFFF` in light, `#1A1E26` in dark).
- **Foreground (Primary Text)**: Deep Charcoal (`#111827` in light, `#F9FAFB` in dark).
- **Foreground Muted**: Slate Gray (`#4B5563` in light, `#9CA3AF` in dark).
- **Borders**: Thin, structured lines (`#D1D5DB` light / `#262C38` dark).
- **Transit Accent**: Amber / Electric Marigold (`#F59E0B` light / `#FBBF24` dark) — used sparingly for primary focal points, live routes, and active ETAs.
- **Status Semantics**:
  - **Success**: Emerald Green (`#10B981`) → Online, Confirmed, Completed.
  - **Warning**: Transit Amber (`#D97706` / `#F59E0B`) → Arriving, Low seats, Waiting.
  - **Danger**: Crimson Red (`#EF4444`) → Emergency SOS, Declined, Cancelled.
  - **Info**: Cobalt Blue (`#3B82F6`) → Route notes, transit notices.

---

## 5. Spacing & Rhythm

Built on an 8-point structural grid with 4-point micro-adjustments:
- `xxs`: `2.dp` (hairline offsets)
- `xs`: `4.dp` (label-to-input gap, icon-to-text gap)
- `sm`: `8.dp` (chip spacing, badge padding, intra-card gaps)
- `md`: `12.dp` (standard card interior padding)
- `lg`: `16.dp` (screen margins, card horizontal padding)
- `xl`: `24.dp` (distance between screen sections)
- `xxl`: `32.dp` (form group gaps)
- `xxxl`: `48.dp` (empty state and hero clearances)

---

## 6. Button Hierarchy & Thumb Zone

### Sizing Scale
- **`Large` (54dp height)**: Primary action buttons and `IshaaraPrimaryAction` — thumb-friendly for drivers and passengers on the move.
- **`Medium` (48dp height)**: Standard actionable buttons (meets WCAG minimum touch target).
- **`Small` (40dp height)**: Secondary compact utility actions (e.g. Call, Details).

### Button Variants
1. **Primary**: Solid dark fill (`colors.foreground`), white text, high contrast.
2. **Secondary**: Neutral surface fill (`colors.surfaceSubtle`), dark text.
3. **Outlined**: 1dp border, transparent background.
4. **Text**: Clean text button for secondary dismissals ("Cancel", "Skip").
5. **Danger**: Solid crimson fill (`colors.danger`) for Emergency SOS or ride cancellation.

---

## 7. Status System

Status is **never communicated by color alone**:

| Status | Color | Visual Representation | Natural Text Label |
| :--- | :--- | :--- | :--- |
| `ONLINE` | Emerald Green | Dot + Text | `Online` |
| `OFFLINE` | Slate Gray | Dot + Text | `Offline` |
| `AVAILABLE` | Emerald Green | Dot + Text | `Available` |
| `COMING` | Transit Amber | Dot + Text | `Coming` |
| `ARRIVING` | Transit Amber | Dot + Text | `Arriving` |
| `ARRIVED` | Transit Amber | Dot + Text | `Driver arrived` |
| `BOARDING` | Transit Amber | Dot + Text | `Boarding` |
| `IN_PROGRESS`| Transit Amber | Dot + Text | `Trip started` |
| `COMPLETED` | Emerald Green | Dot + Text | `Completed` |
| `CANCELLED` | Crimson Red | Dot + Text | `Cancelled` |
| `REJECTED` | Crimson Red | Dot + Text | `Declined` |
| `PENDING` | Transit Amber | Dot + Text | `Waiting for driver` |

---

## 8. Role-Specific UX Principles

### Student / Passenger Experience
Answers the 6 core questions immediately without visual clutter:
1. **Where am I?** → Auto-detected pickup location shown prominently.
2. **Where can I go?** → Natural search bar: "Where are you going?".
3. **Which buses are available?** → Scannable transit cards with Route, Direction, ETA, and Fare.
4. **When will it arrive?** → High-legibility ETA ("8 min away").
5. **How much will it cost?** → Clear fare ("₹20").
6. **What do I do next?** → Obvious primary button ("Request ride").

### Driver / Conductor Experience
Engineered for safety, speed, and glanceability:
- **Zero Distraction**: Large touch buttons (54dp) in the bottom thumb zone.
- **Glanceable Status**: Immediate awareness of current state ("You're online", "Trip ready", "Trip started").
- **No Complex Forms**: Single-tap actions for "Start trip", "Driver arrived", "End trip".
- **Passenger Count at a Glance**: Clear readout of onboard vs open seats.
- **Quick SOS**: Immediate one-tap Emergency SOS.

---

## 9. Accessibility (WCAG 2.1 AA)

- **Touch Targets**: Minimum 48x48dp enforced on all interactive elements.
- **Text Scaling**: Scalable `sp` typography that preserves layout hierarchy under Android system font magnification.
- **Contrast**: Text meets minimum 4.5:1 contrast against surface backgrounds.
- **TalkBack & Semantics**: Explicit `contentDescription` and `Role.Button` attached to all actions.
- **Reduced Motion**: All animations automatically collapse when reduced motion is enabled in system accessibility settings.

---

## 10. Do / Don't Examples

### DO
- ✅ Use natural sentence case: `"Request ride"`, `"Start trip"`, `"8 min away"`.
- ✅ Follow "ONE SCREEN = ONE PRIMARY ACTION" using `IshaaraPrimaryAction`.
- ✅ Show clear recovery actions on error and empty states (`"Try again"`, `"Search again"`).
- ✅ Combine status dots with explicit human text (`● Online`, `● Driver arrived`).

### DON'T
- ❌ Do NOT use futuristic, cyberpunk, or hacker-style brackets (`[ SYSTEM LIVE ]`).
- ❌ Do NOT force ALL-CAPS on normal buttons or headings (`REQUEST RIDE`).
- ❌ Do NOT expose raw exceptions or status codes (`HTTP 409`, `RIDE_CONFLICT`).
- ❌ Do NOT pack dozens of tiny cards into a single screen.
- ❌ Do NOT communicate status with colored dots alone.
