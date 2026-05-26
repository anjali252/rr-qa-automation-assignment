# Test Plan — TMDB Discover Automation
### Rapyuta Robotics QA Assignment

---

## 1. Objective

Validate the functional behaviour of the TMDB Discover demo application (`https://tmdb-discover.surge.sh/`) through automated UI testing. The plan covers filter options (including Rating and Search), pagination, direct URL navigation, and browser API call assertions. Defects identified during testing are documented for discussion.

---

## 2. Scope

### In Scope
- Category navigation (Popular, Trend, Newest, Top Rated)
- Filter options: Type, Genre, Year range, Rating, Search keyword
- Pagination: Next, Previous, direct last page navigation
- Direct URL access to category routes
- Browser network call assertions via CDP / performance logs
- Negative testing for known defects

### Out of Scope
- Backend/database validation
- Performance or load testing
- Mobile browser testing
- Authentication flows (none present on demo site)

---

## 3. Application Under Test

| Property | Value |
|---|---|
| URL | https://tmdb-discover.surge.sh/ |
| Type | React Single Page Application (SPA) |
| Purpose | Demo movie and TV show discovery platform |
| Data Source | TMDB API |

---

## 4. Test Environment

| Property | Value |
|---|---|
| OS | Windows 11 |
| Browser | Google Chrome (latest) |
| Java | 21 |
| Framework | Selenium 4.28.1 + TestNG 7.10.2 |
| Build Tool | Maven 3.x |
| Reporting | ExtentReports 5.1.1 |

---

## 5. Testing Strategy

### 5.1 Approach
The suite uses **functional UI automation** as the primary approach. Each test validates a specific user-facing feature by interacting with the browser and asserting outcomes. Tests are designed to be independent — each test opens a fresh browser session via `@BeforeMethod`.

### 5.2 Test Types

| Type | Description | Applied To |
|---|---|---|
| Positive | Valid inputs, expected successful outcome | TC-F1, TC-F2, TC-F3, TC-F6, TC-P1 |
| Negative | Invalid/edge inputs, expected failure or error | TC-F4, TC-F5, TC-F7, TC-F8, TC-P2 |
| Defect Validation | Intentionally exercises known broken behaviour | TC-F4 (BUG-03), TC-F5 (BUG-05), TC-F7 (BUG-04), TC-F8 (BUG-01), TC-P2 (BUG-02) |

### 5.3 Browser API Testing
Selenium 4 CDP (Chrome DevTools Protocol) and Chrome performance logs are used to:
- Intercept network requests triggered by UI interactions
- Assert that correct TMDB API endpoints are called on filter/pagination actions
- Write captured network events to log files for traceability

---

## 6. Test Cases

### 6.1 Filter Tests

---

#### TC-F1 — Popular Category Loads on Page Load
| Field | Details |
|---|---|
| **Precondition** | Browser is open, no prior navigation |
| **Steps** | 1. Navigate to `https://tmdb-discover.surge.sh/` |
| **Expected Result** | Page loads with 20 movie/TV results visible |
| **Type** | Positive |
| **Design Technique** | Equivalence Partitioning |

---

#### TC-F2 — Switch Between Categories
| Field | Details |
|---|---|
| **Precondition** | App is loaded on Popular page |
| **Steps** | 1. Click **Trend** → verify results <br> 2. Click **Newest** → verify results <br> 3. Click **Top Rated** → verify results |
| **Expected Result** | Each category returns 20 non-empty, valid results |
| **Type** | Positive |
| **Design Technique** | State Transition Testing |

---

#### TC-F3 — Filter by Type (Movie / TV)
| Field | Details |
|---|---|
| **Precondition** | App is loaded |
| **Steps** | 1. Open Type dropdown → select **Movie** → verify results <br> 2. Open Type dropdown → select **TV** → verify results |
| **Expected Result** | Results are non-empty for each type selection |
| **Type** | Positive |
| **Design Technique** | Equivalence Partitioning |

---

#### TC-F4 — Filter by Year Range (Defect Validation — BUG-03)
| Field | Details |
|---|---|
| **Precondition** | App is loaded |
| **Steps** | 1. Set Year From = **2019** <br> 2. Set Year To = **2021** <br> 3. Observe results and verify release years on cards |
| **Expected Result** | All results have release year between 2019 and 2021 |
| **Actual Result** | Results include years outside the selected range |
| **Type** | Negative / Defect Validation |
| **Design Technique** | Boundary Value Analysis |
| **Bug** | BUG-03 |

---

#### TC-F5 — Filter by Rating (Defect Validation — BUG-05)
| Field | Details |
|---|---|
| **Precondition** | App is loaded |
| **Steps** | 1. Note the default result set with no rating filter applied <br> 2. Select a minimum star rating (e.g. **7 stars**) <br> 3. Compare the result set to the unfiltered state |
| **Expected Result** | Results are filtered to show only items with a rating at or above the selected value; result set changes visibly |
| **Actual Result** | Result set is identical to the unfiltered state — the rating filter has no effect |
| **Type** | Negative / Defect Validation |
| **Design Technique** | Equivalence Partitioning, Boundary Value Analysis |
| **Bug** | BUG-05 |

---

#### TC-F6 — Search by Keyword
| Field | Details |
|---|---|
| **Precondition** | App is loaded |
| **Steps** | 1. Enter a known movie or TV show title (e.g. **"Inception"**) into the search input <br> 2. Wait for results to update <br> 3. Verify results contain titles relevant to the search term |
| **Expected Result** | Result cards include titles matching or related to the entered keyword; result count is greater than zero |
| **Type** | Positive |
| **Design Technique** | Equivalence Partitioning |

---

#### TC-F7 — Filter by Genre (Defect Validation — BUG-04)
| Field | Details |
|---|---|
| **Precondition** | App is loaded |
| **Steps** | 1. Open Genre dropdown → select **Action** <br> 2. Observe genres shown on result cards |
| **Expected Result** | All results belong to the Action genre |
| **Actual Result** | Results include Horror, Thriller, Animation and other genres |
| **Type** | Negative / Defect Validation |
| **Design Technique** | Equivalence Partitioning |
| **Bug** | BUG-04 |

---

#### TC-F8 — Direct URL Navigation (Defect Validation — BUG-01)
| Field | Details |
|---|---|
| **Precondition** | None |
| **Steps** | 1. Navigate directly to `https://tmdb-discover.surge.sh/popular` <br> 2. Observe page content <br> 3. Repeat for `https://tmdb-discover.surge.sh/top` |
| **Expected Result** | App loads with results for the respective category |
| **Actual Result** | Surge.sh "Page Not Found" 404 page is displayed |
| **Type** | Negative / Defect Validation |
| **Design Technique** | Error Guessing |
| **Bug** | BUG-01 |

---

### 6.2 Pagination Tests

---

#### TC-P1 — Next / Previous Pagination
| Field | Details |
|---|---|
| **Precondition** | App loaded on Trend category, page 1 |
| **Steps** | 1. Note first result title on page 1 <br> 2. Click **Next** → verify page changes <br> 3. Click **Previous** → verify return to page 1 |
| **Expected Result** | Next shows different results; Previous returns original first title |
| **Type** | Positive |
| **Design Technique** | State Transition Testing |

---

#### TC-P2 — Last Page Edge Case (Defect Validation — BUG-02)
| Field | Details |
|---|---|
| **Precondition** | App loaded on Popular category (57,034 pages) |
| **Steps** | 1. Navigate to Popular category <br> 2. Click last visible page number (e.g. 57034) <br> 3. Observe page content |
| **Expected Result** | Last page loads with valid results |
| **Actual Result** | "Something went wrong! Please try again later." error displayed |
| **Type** | Negative / Defect Validation |
| **Design Technique** | Boundary Value Analysis |
| **Bug** | BUG-02 |

---

## 7. Test Design Techniques

| Technique | Applied To | Rationale |
|---|---|---|
| **Equivalence Partitioning** | TC-F1, TC-F3, TC-F5, TC-F6, TC-F7 | Group valid/invalid inputs into representative classes; rating ≥ 7 as a valid class; known keyword vs. gibberish |
| **Boundary Value Analysis** | TC-F4, TC-F5, TC-P2 | Year range boundaries (2019/2021); minimum and maximum star rating; first and last pages |
| **State Transition Testing** | TC-F2, TC-P1 | App state changes across category switches and pagination |
| **Error Guessing** | TC-F8, TC-P2 | Common SPA failure points — direct URL routing, large dataset last pages |

---

## 8. Defects Found

### BUG-01 — Direct URL Navigation Shows 404

| Field | Details |
|---|---|
| **ID** | BUG-01 |
| **Title** | Direct URL access to `/popular` and `/top` shows Surge.sh 404 page |
| **Steps** | Navigate to `https://tmdb-discover.surge.sh/popular` directly in browser |
| **Expected** | App loads with Popular category results |
| **Actual** | Surge.sh "Page Not Found" page displayed |
| **Root Cause** | React SPA routing not configured for direct URL access — no `_redirects` or fallback routing on Surge |
| **Severity** | High |
| **Status** | Open |
| **Automated** | ✅ TC-F8 |

---

### BUG-02 — Last Page Pagination Shows Error

| Field | Details |
|---|---|
| **ID** | BUG-02 |
| **Title** | Navigating to last page (57034) on Popular shows "Something went wrong" |
| **Steps** | 1. Load Popular category <br> 2. Click last visible page number in pagination |
| **Expected** | Results load for that page |
| **Actual** | "Something went wrong! Please try again later." error with Retry button |
| **Root Cause** | TMDB API likely does not support requests beyond a certain page offset |
| **Severity** | High |
| **Status** | Open |
| **Automated** | ✅ TC-P2 |

---

### BUG-03 — Year Filter Returns Out-of-Range Results

| Field | Details |
|---|---|
| **ID** | BUG-03 |
| **Title** | Year range filter (2019–2021) returns results from outside that range |
| **Steps** | 1. Load app <br> 2. Set Year From = 2019, Year To = 2021 <br> 3. Observe release years on result cards |
| **Expected** | All results have release year between 2019 and 2021 |
| **Actual** | Results include movies/shows from years outside the range |
| **Root Cause** | Year filter parameters may not be correctly applied in the API query |
| **Severity** | Medium |
| **Status** | Open |
| **Automated** | ✅ TC-F4 |

---

### BUG-04 — Genre Filter Returns Mixed Genres

| Field | Details |
|---|---|
| **ID** | BUG-04 |
| **Title** | Genre filter set to Action returns results from multiple genres |
| **Steps** | 1. Load app <br> 2. Select Genre = Action <br> 3. Observe genres displayed on result cards |
| **Expected** | All results belong to the Action genre |
| **Actual** | Results include Horror, Thriller, Animation, Science Fiction and others |
| **Root Cause** | Genre filter may use non-exclusive filtering or TMDB API returns multi-genre content regardless of selection |
| **Severity** | Medium |
| **Status** | Open |
| **Automated** | ✅ TC-F7 |

---

### BUG-05 — Rating Filter Has No Effect

| Field | Details |
|---|---|
| **ID** | BUG-05 |
| **Title** | Selecting a star rating filter does not change the result set |
| **Steps** | 1. Load app <br> 2. Note the default results with no rating selected <br> 3. Select a minimum rating (e.g. 7 stars) <br> 4. Compare results to the default state |
| **Expected** | Results are filtered to items rated at or above the selected minimum; result set visibly changes |
| **Actual** | Result set is identical to the unfiltered state regardless of which rating is selected |
| **Root Cause** | Rating filter value is likely not being passed as a query parameter to the TMDB API, or the parameter name/format is incorrect |
| **Severity** | Medium |
| **Status** | Open |
| **Automated** | ✅ TC-F5 |

---

## 9. Risk and Assumptions

| Risk | Mitigation |
|---|---|
| Demo site content changes between runs | Tests assert structural behaviour (count > 0, title length > 2) rather than specific content values |
| React Select dropdowns are not native `<select>` elements | Custom `selectReactDropdown()` method handles open → type → click pattern |
| Search input may debounce or require explicit trigger | Page method waits for result list to stabilise after input before asserting |
| Last page number changes as TMDB data grows | Test targets last visible page number dynamically via `aria-label` rather than a hardcoded value |
| Chrome version mismatch for CDP DevTools API | Performance log approach used as version-agnostic alternative |
