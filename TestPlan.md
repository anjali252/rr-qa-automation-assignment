# Test Plan — TMDB Discover QA Automation Suite

---

## 1. Introduction

This test plan covers the automated UI and API regression suite for the TMDB Discover demo application hosted at `https://tmdb-discover.surge.sh/`. The suite validates filter behaviour, category switching, search, pagination, and known defects.

---

## 2. Scope

**In scope:**
- Filter functionality: Type, Year Range, Rating, Genre, Search
- Category switching: Popular, Trend, Newest, Top Rated
- Pagination: Next/Previous navigation, last page edge case
- Direct URL navigation
- Browser-level API call validation via CDP performance logs

**Out of scope:**
- Backend/API testing independent of the UI
- Mobile browser testing
- Accessibility testing
- Performance/load testing

---

## 3. Test Objectives

- Verify all filter controls return non-empty, contextually correct results
- Confirm rating filter changes result cards when different thresholds are applied
- Document known defects with reproducible automated assertions
- Validate pagination navigates correctly and handles edge cases gracefully
- Ensure no regression in positive flows across deployments

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
The suite uses **functional UI automation** as the primary approach. Each test validates a specific user-facing feature by interacting with the browser and asserting outcomes. Tests are independent — each opens a fresh browser session via `@BeforeMethod`.

### 5.2 Test Types

| Type | Description | Applied To |
|---|---|---|
| Positive | Valid inputs, expected successful outcome | TC-F1, TC-F2, TC-F3, TC-F5, TC-F6, TC-P1 |
| Negative | Invalid/edge inputs, expected failure or error | TC-F4, TC-F7, TC-F8, TC-P2 |
| Defect Validation | Intentionally exercises known broken behaviour | TC-F4 (BUG-03), TC-F7 (BUG-04), TC-F8 (BUG-01), TC-P2 (BUG-02) |

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
| **Steps** | 1. Click **Trend** → verify results <br> 2. Click **Newest** → verify results <br> 3. Click **Top rated** → verify results |
| **Expected Result** | Each category returns non-empty, valid results |
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
| **Steps** | 1. Set Year From = **2019** <br> 2. Set Year To = **2021** <br> 3. Observe years shown on result cards |
| **Expected Result** | All results have a release year within 2019–2021 |
| **Actual Result** | Results from years outside the range are included (e.g. 1997, 2011) |
| **Type** | Negative / Defect Validation |
| **Design Technique** | Boundary Value Analysis |
| **Bug** | BUG-03 |

---

#### TC-F5 — Search by Keyword
| Field | Details |
|---|---|
| **Precondition** | App is loaded |
| **Steps** | 1. Type **"Batman"** into the search input <br> 2. Wait for results to update <br> 3. Verify at least one result title contains "Batman" |
| **Expected Result** | Results are returned and at least one title matches the search term |
| **Type** | Positive |
| **Design Technique** | Equivalence Partitioning |

---

#### TC-F6 — Filter by Rating
| Field | Details |
|---|---|
| **Precondition** | App is loaded |
| **Steps** | 1. Note baseline results (no filter) <br> 2. Apply **1-star** minimum rating → note result count <br> 3. Apply **5-star** minimum rating → note result count <br> 4. Compare the two result sets |
| **Expected Result** | Rating filter changes result cards — stricter threshold (5-star) returns fewer results than a loose threshold (1-star); the two sets are not identical |
| **Type** | Positive |
| **Design Technique** | Boundary Value Analysis, Equivalence Partitioning |

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
| **Expected Result** | Next shows different results; Previous restores the original first title |
| **Type** | Positive |
| **Design Technique** | State Transition Testing |

---

#### TC-P2 — Last Page Edge Case (Defect Validation — BUG-02)
| Field | Details |
|---|---|
| **Precondition** | App loaded on Popular category (57,034 pages) |
| **Steps** | 1. Navigate to Popular category <br> 2. Click last visible page number <br> 3. Observe page content |
| **Expected Result** | Last page loads with valid results |
| **Actual Result** | TMDB API returns HTTP 400 for the out-of-range page number; app shows 0 results |
| **Type** | Negative / Defect Validation |
| **Design Technique** | Boundary Value Analysis |
| **Bug** | BUG-02 |

---

## 7. Test Design Techniques

| Technique | Applied To | Rationale |
|---|---|---|
| **Equivalence Partitioning** | TC-F1, TC-F3, TC-F5, TC-F6, TC-F7 | Representative input classes for filters; valid keyword vs. gibberish; 1-star vs. 5-star as distinct rating partitions |
| **Boundary Value Analysis** | TC-F4, TC-F6, TC-P2 | Year range boundaries (2019/2021); minimum and maximum star rating (1 and 5); last page of pagination |
| **State Transition Testing** | TC-F2, TC-P1 | App state changes across category switches and pagination navigation |
| **Error Guessing** | TC-F8, TC-P2 | Common SPA failure points — direct URL routing without a rewrite rule; excessively large page numbers |

---

## 8. Defects

---

#### BUG-01 — Direct URL `/popular` and `/top` shows 404

| Field | Details |
|---|---|
| **Steps** | Navigate directly to `https://tmdb-discover.surge.sh/popular` |
| **Expected** | App loads with results |
| **Actual** | Surge.sh "Page Not Found" 404 page displayed |
| **Root Cause** | SPA deep links not configured — Surge.sh needs a `200.html` rewrite file so all paths serve `index.html` |
| **Severity** | High |
| **Status** | Open |
| **Automated** | ✅ TC-F8 |

---

#### BUG-02 — Last page pagination shows error

| Field | Details |
|---|---|
| **Steps** | Popular category → click last visible page number (e.g. 57,034) |
| **Expected** | Results load for the last page |
| **Actual** | TMDB API returns HTTP 400 for `page=57050`; app shows 0 results with no clear error message |
| **Root Cause** | No upper bound on page number — app passes the raw last-page value directly to the TMDB API without checking it against the API's maximum allowed page |
| **Severity** | High |
| **Status** | Open |
| **Automated** | ✅ TC-P2 |

---

#### BUG-03 — Year filter returns out-of-range results

| Field | Details |
|---|---|
| **Steps** | Set Year filter to 2019–2021 → observe result card years |
| **Expected** | All results within 2019–2021 |
| **Actual** | Results from years outside the range included (e.g. 1997, 2011, 2015) |
| **Root Cause** | Year range values likely not being passed correctly to the TMDB API `primary_release_date.gte` / `primary_release_date.lte` parameters |
| **Severity** | Medium |
| **Status** | Open |
| **Automated** | ✅ TC-F4 |

---

#### BUG-04 — Genre filter returns mixed genres

| Field | Details |
|---|---|
| **Steps** | Select Genre = Action → observe genres on result cards |
| **Expected** | All results belong to the Action genre |
| **Actual** | Results include Horror, Thriller, Animation, Science Fiction etc. |
| **Root Cause** | `with_genres` parameter likely missing or incorrect in the TMDB API request when genre filter is applied |
| **Severity** | Medium |
| **Status** | Open |
| **Automated** | ✅ TC-F7 |

---

## 9. Risk and Assumptions

| Risk | Mitigation |
|---|---|
| Demo site content changes between runs | Tests assert structural behaviour (count > 0, title length > 2) rather than specific content values |
| React Select dropdowns are not native `<select>` elements | Custom `selectReactDropdown()` method handles open → type → click pattern |
| Search input may debounce or require explicit trigger | Page method waits for result list to stabilise after input before asserting |
| Last page number changes as TMDB data grows | Test targets last visible page number dynamically rather than a hardcoded value |
| Chrome version mismatch for CDP DevTools API | Performance log approach used as version-agnostic alternative |
| WebDriverManager network timeout on restricted networks | `.timeout(10).avoidResolutionCache()` configured in BaseTest; set `.driverVersion("148")` to skip network lookup entirely if needed |