# Security Review — Bug Report & Learning Notes

Project: `sb-ecomm` (Spring Boot 3.5.10, Spring Security, JWT in a cookie, Postgres)
Scope: the Spring Security implementation, plus the payment path it protects.
Status: **changes are in your working tree, not committed.**

---

## 0. Read this first

### How much can you trust "fixed"?

Every bug below has one of these labels. I use them literally.

| Label | Meaning |
|---|---|
| **VERIFIED (red→green)** | A test failed on the old code and passes on the new code. Strongest evidence. |
| **VERIFIED (green + mutation)** | Test passes on the new code, and when I deliberately re-broke the fix the test failed. I could not get a clean "red" on the old code (reason given per bug). |
| **PARTIALLY VERIFIED** | Some of the behaviour is tested; the rest is not. The gap is stated. |
| **NOT VERIFIED** | Changed, but I could not test it. |
| **NOT FIXED** | Investigated, deliberately left alone. |

### What was and wasn't run

- **Run:** 30 new tests in `src/test/java/com/ecomm/sb_ecomm/security/` — **30 pass, 0 fail**.
  Command: `mvn test -Dmaven.compiler.proc=full -Dtest='SecurityHardeningWebTest,JwtUtilsTest,MockGatewayGatingTest,PaymentOwnershipTest,SignupUserJsonTest,ConfigProfilesTest'`
- **Not run:** the app itself against Postgres. Docker is not running on this machine, so I could not start
  the real application, and I could **not run your existing `SbEcommApplicationTests`** (it needs a Testcontainers
  Postgres, and it contains your concurrency tests). Run it yourself before merging.
- **Never seen in a browser or Postman:** sign-in, cookies, Swagger UI.

### Corrections to what I told you earlier

1. **I said secrets were "committed". That was wrong.** `application.properties` is in your `.gitignore` (line 29)
   and I searched history (`git log --all -S`) — the secret was never committed. The real problem is different and
   smaller: the secrets were hand-typed, weak, shared by every environment, and there was no separation between
   "dev" and "anything else". Bug 4 is written with the corrected picture.
2. **I overwrote your local `application.properties`.** It was gitignored, so git has no copy. Nothing of value
   was lost: every value is now in `application-dev.properties`, except the old JWT secret string, which I replaced
   with a random one (the old one still exists in `src/test/resources/application.properties`, which I did not touch).
3. **I edited `.gitignore`** (added `application-*.properties` / `application-*.yml`). Without it, my new
   `application-dev.properties` — which holds a dev JWT secret and DB password — would have been committed by a
   plain `git add .`. Revert that line if you want a different policy.

### Behaviour changes that can break clients or your workflow

| Change | Who is affected |
|---|---|
| `/api/auth/signin` no longer returns the token in the JSON body | Any frontend reading `jwtToken` from the response |
| `/api/auth/signup` ignores `roles`; everyone is `ROLE_USER` | Anything that registered sellers/admins through signup |
| Cookie is `HttpOnly` | JavaScript that read the cookie (`document.cookie`) |
| Cookie is `SameSite=Lax` | A frontend on a *different site* (not just a different port) won't send it |
| `/api/mock-gateway/**` needs `ROLE_ADMIN` and `mock.gateway.enabled=true` | Your manual payment-completion flow (do it while signed in as `admin`) |
| Outside the `dev` profile: no secrets, no seed users, Swagger off, `ddl-auto=validate` | Any non-dev deployment: you must set env vars (see Bug 4) |
| Postman + `Secure` cookies | Dev profile sets `cookieSecure=false`; other profiles need HTTPS |

### Summary

| # | Bug | Severity | Status |
|---|---|---|---|
| 1 | Public signup lets the caller choose their own roles (self-service admin) | Critical | VERIFIED (green + mutation) |
| 2 | Public endpoint that forges "payment captured" webhooks | Critical | VERIFIED (green + mutation) |
| 3 | Auth cookie readable by JavaScript, no `Secure`/`SameSite`; token also in response body | High | VERIFIED (green + mutation) |
| 4 | One weak, hand-typed secret set for every environment; demo admin accounts always created | High | PARTIALLY VERIFIED |
| 5 | Live JWT written to stdout on every request | High | VERIFIED (red→green) |
| 6 | Any user could start a payment on anyone's order; `getPayment` used the wrong id | High | VERIFIED (red→green) |
| 7 | Sign-out didn't expire the cookie; cookie and token lifetimes disagreed | Medium | VERIFIED (green + mutation) |
| 8 | `/api/auth/user` crashed with a 500 (NPE) when signed out | Medium | VERIFIED (green + mutation) |
| 9 | `validateToken` threw on a bad signature instead of returning `false` | Medium | VERIFIED (red→green) |
| 10 | Dev tooling paths open in every environment; Swagger public; `web.ignoring()` misuse | Medium | PARTIALLY VERIFIED |
| 11 | Access-denied (403) returned an empty body, unlike the 401 | Low | VERIFIED (green) |
| 12 | JWT filter registered twice (servlet container *and* security chain) | Low | PARTIALLY VERIFIED |
| 13 | Password/username rules on `SignupUser` were never enforced (`@Valid` missing) | Medium | VERIFIED (green) |
| 14 | Build doesn't compile on JDK 25 (Lombok annotation processing) | Build | NOT FIXED |

**Not related to my changes but you should know:** in my first answer I listed your `springdoc` downgrade
(3.1.1 → 2.9.1) as a probable fix. That was *your* uncommitted change; I did not touch or verify it.

---

## Bug 1 — Public signup lets the caller choose their own roles

1. **Title:** Privilege escalation through `POST /api/auth/signup`.
2. **Where:** `auth/controllers/AuthController.java` → `signupUser(...)`; `auth/payload/request/SignupUser.java` (field `roles`).
3. **Supposed to do:** register an ordinary customer account.
4. **Actually did:** read a `roles` array from the request body and mapped `"admin"`, `"seller"`, `"user"` to real roles.
   `curl -X POST /api/auth/signup -d '{"username":"x","password":"...","email":"...","roles":["admin"]}'` created an admin.
   `/api/auth/**` is `permitAll`, so no login was needed.
5. **Root cause:** the server let the client decide a *privilege*. The DTO exposed a security-critical field
   (`roles`) as ordinary input — the general pattern is called **mass assignment**. It likely started as
   tutorial/demo convenience ("let me test admin routes quickly") and was never removed.
6. **How found:** reading `signupUser` during the security review, after seeing that the signup path is public in `WebSecurityConfig`.
7. **Fix:**
   - `SignupUser`: removed the `roles` field. Added `@JsonIgnoreProperties("roles")` so an old client that still
     sends `roles` is ignored rather than rejected (see "related risks" for why this matters here).
   - `AuthController.signupUser`: deleted the role-mapping `switch`; it now always assigns `ROLE_USER`.
   - Added `@Valid` (see Bug 13).
8. **Why it works:** there is no longer any code path from request data to a role. The only input that reaches the
   role decision is a constant. You can't validate your way out of a design where the client controls the answer;
   you remove the client's ability to influence it.
9. **Concept:** *never trust client input for authorization decisions*; mass assignment; least privilege
   (default to the lowest role); authentication ≠ authorization.
10. **Test:** added `SecurityHardeningWebTest.signup_ignoresRequestedRoles_andAlwaysCreatesPlainUser` and
    `SignupUserJsonTest.legacyRolesField_isIgnored_evenWithAStrictObjectMapper`.
11. **Verification:** tests pass on the fixed code. I re-broke the fix (signup granting `ROLE_ADMIN`) and the test
    failed → the test genuinely detects this bug. **I could not show a red run on the original code:** at baseline
    that whole test class failed at startup (Bug 4's seeder crashed against mocked repositories), so it said
    nothing about signup. Hence "green + mutation", not "red→green".
12. **Related risks:**
    - **There is now no way to create a seller or admin except the dev seeder.** You need an admin-only endpoint
      (e.g. `POST /api/admin/users/{id}/roles`). Not built.
    - **Audit any existing database** for accounts created through signup with elevated roles.
    - `AppConfigs.objectMapper()` returns a bare `new ObjectMapper()`. That replaces Spring Boot's tuned mapper and
      has `FAIL_ON_UNKNOWN_PROPERTIES = true`, so *any* extra JSON field on *any* endpoint returns 400. That is why
      I used `@JsonIgnoreProperties` instead of just deleting the field. I did not change `AppConfigs`.
    - Signup still returns different messages for "username exists" vs "email exists" (account enumeration). Not fixed.

---

## Bug 2 — Public endpoint that forges "payment captured" webhooks

1. **Title:** Anyone can mark any payment as paid via the mock gateway.
2. **Where:** `auth/WebSecurityConfig.java` → `filterChain` (`/api/mock-gateway/**` → `permitAll`);
   `gateway/MockGatewayController.java` → `completePayment(...)`.
3. **Supposed to do:** a development stand-in for a real payment provider, so you can simulate "customer paid".
4. **Actually did:** `POST /api/mock-gateway/complete/{reference}?outcome=CAPTURED` — with **no login** — built a webhook,
   **signed it with your real signing secret**, and posted it to your own `/api/webhooks/mock`. The webhook
   handler then correctly trusted it, because the signature was valid. Any caller who knows or guesses a gateway
   reference could complete a payment without paying.
5. **Root cause:** a *trusted signer* was exposed to the public. The webhook design is sound (HMAC, timestamp window,
   de-duplication) — but its security depends entirely on only the payment provider being able to sign. The mock
   endpoint handed the signing capability to everyone. It was also always compiled in, in every environment.
6. **How found:** while reading `WebSecurityConfig`, the `permitAll` on `/api/mock-gateway/**` stood out; the
   controller confirmed it signs with `mock.gateway.signing-secret`.
7. **Fix:**
   - `MockGatewayController`: `@ConditionalOnProperty(name="mock.gateway.enabled", havingValue="true")` — the
     class does not exist as a bean unless switched on (default `false`; the `dev` profile sets `true`).
   - `WebSecurityConfig`: `/api/mock-gateway/**` → `hasRole("ADMIN")` (was `permitAll`).
8. **Why it works:** two independent locks. In a non-dev deployment the endpoint isn't there at all (404). Where it
   is enabled, only an authenticated admin can reach it. Either lock alone closes the hole; together, forgetting one
   is survivable (**defense in depth**).
9. **Concept:** trust boundaries; a signature only proves "someone with the key signed this"; test/mock tooling must
   not ship enabled; defense in depth; fail-closed defaults.
10. **Test:** `MockGatewayGatingTest` (3 tests) and `SecurityHardeningWebTest.mockGateway_rejectsAnonymousCallers`,
    `mockGateway_rejectsNonAdminUsers`.
11. **Verification:**
    - **red→green** for the gating: at baseline `controllerIsAbsentByDefault` and
      `controllerIsAbsentWhenExplicitlyDisabled` failed (the controller existed); they pass now.
    - For the route rule: re-broke it (`permitAll`) and both web tests failed, and the failure showed the request
      *reaching the controller and trying to call the webhook* — a live demonstration of the attack path.
12. **Related risks:**
    - `/api/webhooks/**` is still public — correctly, since it authenticates by signature. But that means **the
      signing secret is now the only thing protecting payments**. Rotate it for any real environment
      (`mock_whsec_67890` is a placeholder).
    - The mock lets *any admin* complete *any* payment. Fine for dev; a real provider integration replaces it.
    - `MockGatewayServicesImpl` returns a checkout URL `/api/mock-gateway/checkout/{ref}` that **no controller serves**.
      Not investigated further.

---

## Bug 3 — Auth cookie readable by JavaScript, no `Secure` / `SameSite`, token duplicated in the body

1. **Title:** Weak session-cookie flags and token leakage in the sign-in response.
2. **Where:** `auth/jwt/JwtUtils.java` → `generateJwtCookie(...)`; `auth/controllers/AuthController.java` → `authenticateUser(...)`.
3. **Supposed to do:** log the user in with a token the browser holds but page scripts can't steal.
4. **Actually did:** built the cookie with `.httpOnly(false)` and no `Secure` or `SameSite`, *and* put the same
   token string in the JSON response body (`UserInfoResponse(..., jwtCookie.toString(), ...)`).
5. **Root cause:** `httpOnly(false)` defeats the main reason to use a cookie. With it, any XSS bug (or any script
   on the page) can read the JWT and impersonate the user until it expires. Returning the token in the body gives
   JavaScript a second way to get it, which makes the cookie's protection moot. Missing `SameSite` matters because
   **CSRF protection is disabled** (`csrf.disable()`), which is only safe for cookie auth if the browser refuses
   cross-site sends. Missing `Secure` lets the cookie travel over plain HTTP.
6. **How found:** reading `generateJwtCookie` and the sign-in controller.
7. **Fix:** `JwtUtils` now builds every auth cookie through one helper: `HttpOnly`, `Secure` (from
   `spring.app.ecom.cookieSecure`, **default `true`**), `SameSite=Lax`, path `/api`. Sign-in returns
   `new UserInfoResponse(id, username, roles)` without the token.
8. **Why it works:** `HttpOnly` makes the cookie invisible to JavaScript, so XSS can no longer exfiltrate it.
   `SameSite=Lax` stops browsers attaching it to cross-site POSTs, which is the CSRF vector. `Secure` keeps it off
   plain HTTP. Removing it from the body closes the second leak. The secure default means forgetting to configure
   it fails *safe*.
9. **Concept:** cookie security attributes; XSS vs CSRF (different attacks, different defences); why "disable CSRF"
   is only valid with the right cookie policy; secure-by-default configuration.
10. **Test:** `SecurityHardeningWebTest.signin_setsHardenedCookie_andDoesNotLeakTokenInBody`,
    `JwtUtilsTest.authCookie_isHttpOnlySecureSameSite_andLivesAsLongAsTheToken`.
11. **Verification:** pass on the fixed code. Re-broke `httpOnly(true)→false` and 4 tests failed. No clean red on
    the old code for the same reasons as Bug 1 (web class couldn't start; the `JwtUtils` test errored on a field
    that didn't exist yet — an error, not evidence about the old behaviour).
12. **Related risks:**
    - **`SameSite=Lax` breaks a frontend hosted on a different *site*** (different domain, not just a different
      port — `localhost:3000` → `localhost:8080` is same-site and fine). A cross-site SPA needs `SameSite=None; Secure`
      **and** CORS with credentials. I saw no CORS config in the project; I did not test any frontend.
    - `Lax` does not stop attacks from *within* the same site. A CSRF token would be stricter. Not added.
    - The dev profile sets `cookieSecure=false` so plain-HTTP localhost works. It must never be copied elsewhere.

---

## Bug 4 — One weak, hand-typed secret set for every environment; demo admins always created

1. **Title:** No separation between dev configuration and everything else.
2. **Where:** `src/main/resources/application.properties` (JWT secret, DB password, gateway keys, `ddl-auto=create-drop`);
   `auth/WebSecurityConfig.java` → `initData()` (seeder).
3. **Supposed to do:** configure the app; seed a few accounts for local testing.
4. **Actually did:** a single file held throwaway-quality values (DB password `12345`, a JWT secret starting with the
   English phrase "mySecretKey…", public-looking gateway keys). Nothing distinguished "my laptop" from "a server".
   `initData()` ran on **every** startup in **every** environment and created `admin/adminPass`, `user1/password1`,
   `seller1/password2`. `ddl-auto=create-drop` **drops the whole schema on shutdown**.
5. **Root cause:** development shortcuts with no environment boundary. If this build were ever deployed, it would
   ship well-known admin credentials, a guessable token-signing key (anyone with the key can mint a valid token
   for any user), and a database that erases itself.
6. **How found:** reading `application.properties` and `initData`.
7. **Fix:**
   - `application.properties` (base): secrets are now placeholders — `${JWT_SECRET}`, `${DB_URL}`, `${DB_USERNAME}`,
     `${DB_PASSWORD}`, `${MOCK_GATEWAY_API_KEY}`, `${MOCK_GATEWAY_SIGNING_SECRET}` — with **no defaults**.
     Safe defaults everywhere else: `cookieSecure=true`, `mock.gateway.enabled=false`, `app.seed-data.enabled=false`,
     Swagger off, `ddl-auto=validate`.
   - New `application-dev.properties`: the local values, a **freshly generated random 48-byte JWT secret**, and the
     dev-only switches turned on. `spring.profiles.default=dev` makes plain `mvn spring-boot:run` behave as before.
   - `initData` is `@ConditionalOnProperty("app.seed-data.enabled")`.
8. **Why it works:** Spring resolves `${JWT_SECRET}` at read time and throws if it is missing, so a non-dev start
   **fails fast** instead of quietly running with a bad secret. `spring.profiles.default` only applies when no profile
   is explicitly active, so setting `SPRING_PROFILES_ACTIVE=prod` switches off every dev convenience at once.
9. **Concept:** twelve-factor config (secrets from the environment); Spring profiles; fail-fast; least-surprise
   defaults; separating dev conveniences from production.
10. **Test:** `ConfigProfilesTest` (3 tests). It loads the **real** `src/main/resources` files (not the test copy).
11. **Verification — PARTIAL.** Verified: the default run resolves the dev profile, a long random secret, seeding on;
    a `prod` profile throws on `JWT_SECRET` / `DB_PASSWORD` / gateway secret, has cookies secure, mock/seed/Swagger off,
    `validate`; and an env-supplied secret is picked up. **Not verified:** the seeder itself being skipped in a full
    boot, and the app connecting to Postgres — I couldn't start the app. Also, those files are gitignored, so this
    test **skips** (rather than fails) on a fresh clone.
12. **Related risks:**
    - **No config file is tracked in git at all** (`application*.properties` are ignored). A teammate or CI cloning
      the repo has no configuration and can't run the app. Consider committing a sanitized `application.properties`
      (placeholders only) or an `application.properties.example`.
    - Rotate the DB password (`12345`) if that database is reachable by anyone else.
    - `src/test/resources/application.properties` still holds the old JWT secret and gateway values. They are
      test-only and untracked, but I left them.
    - `ddl-auto=validate` outside dev assumes a schema exists. You have no Flyway/Liquibase migrations. Adding them
      is the proper fix; not done.
    - This does not deploy anything — no Dockerfile/CI was inspected.

---

## Bug 5 — Live JWT written to stdout on every request

1. **Title:** Token logging.
2. **Where:** `auth/jwt/JwtUtils.java` → `getJwtFromCookie(...)` (and `validateToken(...)` printed too).
3. **Supposed to do:** return the token from the request cookie.
4. **Actually did:** `System.out.println(cookie.getName() + " : " + cookie.getValue())` — the full, live, signed
   token went to the console (and any log collector) on every authenticated request.
5. **Root cause:** debugging code left in. A bearer token is a password with an expiry: anyone who can read the
   logs can replay it. `System.out` also bypasses log levels, so you can't turn it off.
6. **How found:** reading `JwtUtils`.
7. **Fix:** deleted both `println` calls. Errors still go through the SLF4J logger, which logs the *reason* but
   never the token.
8. **Why it works:** the secret is simply never written anywhere.
9. **Concept:** treat credentials as secrets in logs; never log request secrets; use a logging framework instead of
   `System.out`.
10. **Test:** `JwtUtilsTest.readingTheCookie_doesNotWriteTheTokenToStdout` (redirects `System.out` and asserts the
    token isn't in it).
11. **Verification: VERIFIED (red→green).** Failed on the old code, passes now.
12. **Related risks:** the `AuthTokenFilter` `logger.debug("Authentication filter.Java: {}", jwt)` still logs the
    token at DEBUG level. I didn't change it; it is off by default, but don't enable DEBUG in production. Existing
    logs on your machine may already contain tokens.

---

## Bug 6 — Any user could start a payment on anyone's order; `getPayment` used the wrong id

1. **Title:** Missing ownership check (an IDOR, "insecure direct object reference") in the payment service.
2. **Where:** `payment/services/PaymentServicesImpl.java` → `initiatePayment(...)` and `getPayment(...)`;
   `payment/repository/PaymentRepository.java`.
3. **Supposed to do:** `initiatePayment`: start payment for *the caller's* order. `getPayment(userId, orderId)`:
   return the payment for that user's order.
4. **Actually did:**
   - `initiatePayment` used `orderRepository.findById(orderId)`. `/api/user/**` only checks *role*, not *which
     user*, so any logged-in user could pass someone else's `orderId`, create a `Payment` for them and attach it
     to their order.
   - `getPayment` was `return null;`; the version in your working tree called
     `findByIdAndOrderId(userId, orderId)` — passing the **user id as the payment id**.
5. **Root cause:** *authentication* (you're logged in) and *authorization* (this resource is yours) are separate
   checks, and the second was missing at the data-access layer. Your `OrderServiceImpl` already does it right
   (`findByIdAndUserId`); the payment service didn't copy the pattern. The `getPayment` bug is a classic of methods
   with several positional `Long` parameters: the compiler can't tell a user id from a payment id from an order id.
   Also, `AuthUtils` was injected but the constructor parameter was renamed `authUtils1` and never assigned.
6. **How found:** searching for where `AuthUtils` was used (grep showed the payment service never used it) and reading
   `initiatePayment`.
7. **Fix:**
   - Restored an `authUtils` field. `initiatePayment` now calls `orderRepository.findByIdAndUserId(orderId,
     authUtils.loggedInUserId())`. Someone else's order throws `ResourceNotFoundException` (404) — the same response
     as a non-existent order, so it doesn't reveal which order ids exist.
   - `getPayment` now loads the order via `findByIdAndUserId(orderId, userId)`, returns its payment, and throws
     `ResourceNotFoundException("Payment"…)` if the order has none.
   - Removed the buggy `findByIdAndOrderId` repository method (unused, and semantically wrong).
8. **Why it works:** the ownership condition is part of the query itself, so a non-owner's row is never loaded.
   It can't be forgotten later because there's no unscoped path left in this class.
9. **Concept:** authorization at the object level (OWASP "Broken Object Level Authorization"); scoping queries to
   the current user; why 404 beats 403 for hidden resources; strongly typed IDs vs positional `Long` parameters.
10. **Test:** `PaymentOwnershipTest` (5 tests).
11. **Verification: VERIFIED (red→green).** On the old code `initiatePayment_forSomeoneElsesOrder_isRejected…`
    failed (a payment was created for another user's order) and `getPayment_returnsThePaymentOfMyOrder` failed.
    Both pass now. Note these are **unit tests with mocked repositories** — they prove the service asks the right
    question, not that Postgres returns the right rows. The Docker-based integration test would cover that.
12. **Related risks (read, not tested):**
    - `initiatePayment` doesn't check whether the order **already has a payment**. Calling it twice looks like it
      creates a second `Payment`. Not fixed.
    - `getPayment` isn't exposed by any controller yet; when you add it, take the user from the security context,
      not from a request parameter.
    - I spot-checked `AddressServicesImpl.setAddressAsDefault` (correctly scoped) and the order lookup helpers,
      but I did **not** audit every endpoint for this bug class (cart and admin paths in particular).

---

## Bug 7 — Sign-out didn't expire the cookie; cookie and token lifetimes disagreed

1. **Title:** Ineffective logout and mismatched lifetimes.
2. **Where:** `auth/jwt/JwtUtils.java` → `getCleanCookie()` and `generateJwtCookie(...)`.
3. **Supposed to do:** sign-out removes the session; the cookie should live as long as the token.
4. **Actually did:** `getCleanCookie()` produced a cookie with an empty value and **no `Max-Age`** (a session
   cookie, not a deletion). The auth cookie had `maxAge` 24 h while the token inside expired after 50 min
   (`jwtExpirationMs=3000000`).
5. **Root cause:** to delete a cookie you must send it back already expired (`Max-Age=0`). The two lifetimes were
   configured independently in two places (one hard-coded).
6. **How found:** reading `JwtUtils` next to `application.properties`.
7. **Fix:** `getCleanCookie()` uses `maxAge(Duration.ZERO)` with the same attributes; the login cookie's `maxAge`
   is now `Duration.ofMillis(jwtExpirationMs)`.
8. **Why it works:** the browser drops an expired cookie immediately, and there is now one source of truth for
   lifetime.
9. **Concept:** how HTTP cookie deletion works; single source of truth for configuration.
10. **Test:** `SecurityHardeningWebTest.signout_expiresTheCookie`, `JwtUtilsTest.cleanCookie_expiresImmediately`,
    `JwtUtilsTest.authCookie_…LivesAsLongAsTheToken`.
11. **Verification: green + mutation.** Pass now; the HttpOnly mutation also failed the cookie tests. **No baseline
    evidence** — the old-code runs errored before reaching these assertions — so the claim about the old behaviour
    comes from reading Spring's `ResponseCookie`, not from running it.
12. **Related risks:** **sign-out is still only client-side.** A token that has been stolen stays valid until it
    expires; the server keeps no revocation list. Mitigations (short expiry + refresh tokens, or a token deny-list)
    are not implemented. Not fixed.

---

## Bug 8 — `/api/auth/user` crashed with a 500 when signed out

1. **Title:** NullPointerException on an unauthenticated request.
2. **Where:** `auth/WebSecurityConfig.java` (`/api/auth/**` is `permitAll`) + `AuthController.getUserDetails(...)`.
3. **Supposed to do:** return the current user's details.
4. **Actually did:** the route was public, so an anonymous request reached the method, `Authentication` was `null`,
   and `authentication.getPrincipal()` threw an NPE → HTTP 500. (The endpoint's own docs said "called without one,
   this endpoint fails".)
5. **Root cause:** the blanket `permitAll` on `/api/auth/**` was meant for sign-in/sign-up, and swallowed an
   endpoint that needs a login. The method also assumed a non-null argument.
6. **How found:** reading the controller's Swagger text and the matcher list together.
7. **Fix:** added `.requestMatchers("/api/auth/user").authenticated()` **before** `/api/auth/**`.
8. **Why it works:** Spring Security uses the **first matching rule**, so the specific rule must precede the general
   one. Anonymous callers now get a clean 401 from the entry point before the controller runs.
9. **Concept:** matcher ordering (first match wins); a 5xx for a client mistake is a bug — it should be a 4xx.
10. **Test:** `currentUserEndpoint_withoutCookie_is401_notAServerError`, `currentUserEndpoint_withValidCookie_returnsTheUser`,
    `tamperedCookie_isTreatedAsAnonymous`.
11. **Verification: green + mutation.** Removing the new matcher made the tests fail with
    `NullPointerException` — reproducing the original crash exactly.
12. **Related risks:** `/api/auth/username` is still public by design (returns `""` when signed out).

---

## Bug 9 — `validateToken` threw on a bad signature

1. **Title:** Uncaught `SignatureException` in JWT validation.
2. **Where:** `auth/jwt/JwtUtils.java` → `validateToken(String)`.
3. **Supposed to do:** return `true`/`false`.
4. **Actually did:** it caught `Malformed`, `Expired`, `Unsupported`, and `IllegalArgument` — but **not** a bad
   signature. A token signed with the wrong key made it *throw* `SignatureException`.
5. **Root cause:** an incomplete exception list for a library that has a **hierarchy** (`SignatureException` and the
   others all extend `JwtException`).
6. **How found:** by my own test, written to check "a forged token is rejected without throwing". It failed on
   the old code with "JWT signature does not match locally computed signature."
7. **Fix:** added `catch (JwtException e)` after the specific catches (order matters: subclasses first); switched
   to the non-deprecated `parseSignedClaims(...).getPayload()`.
8. **Why it works:** every rejection the parser can raise is now a `JwtException`, and all are converted to `false`.
9. **Concept:** exception hierarchies; catch specific-to-general; a method's contract must hold for *all* bad input,
   especially attacker-controlled input.
10. **Test:** `JwtUtilsTest.tokenSignedWithAnotherKey_isRejected_withoutThrowing`, `expiredToken_isRejected`,
    `validToken_isAccepted`.
11. **Verification: VERIFIED (red→green).**
12. **Related risks:** the practical impact was **low**: `AuthTokenFilter` wraps everything in `catch (Exception)`,
    so the request still ended as anonymous → 401. That safety net hid the bug, and is itself a smell (it also
    swallows real errors like a DB outage). I left the filter's catch-all as is. The token is still parsed twice per
    request (`validateToken` then `getUsernameFromJwtToken`) — minor inefficiency, not fixed.

---

## Bug 10 — Dev tooling open in every environment; Swagger public; `web.ignoring()`

1. **Title:** Unneeded and over-broad public routes.
2. **Where:** `auth/WebSecurityConfig.java` → `filterChain` and `webSecurityCustomizer`.
3. **Supposed to do:** expose only what must be public.
4. **Actually did:** `/h2-console/**` and `/api/test/**` were `permitAll` (you use Postgres; no test controller
   exists). Legacy Springfox paths were bypassed with `web.ignoring()`, which removes those requests from the
   **entire** filter chain, including security headers. `frameOptions.sameOrigin()` existed only to support the H2
   console. Swagger was public in all environments.
5. **Root cause:** leftovers from earlier setups (H2, Springfox) that no one pruned; a very wide default allow-list.
6. **How found:** comparing the matcher list against controllers and dependencies (`grep`, `pom.xml`).
7. **Fix:** removed `/h2-console/**`, `/api/test/**`, the `WebSecurityCustomizer` bean and the custom frame-options
   (Spring's default `X-Frame-Options: DENY` returns). Swagger UI/docs paths (`/swagger-ui.html`, `/swagger-ui/**`,
   `/webjars/**`, `/v3/api-docs/**`) are `permitAll` **inside** the chain, and the springdoc endpoints are
   **disabled outside the dev profile**.
8. **Why it works:** an unlisted path falls to `.anyRequest().authenticated()`. Keeping Swagger inside the chain means
   headers still apply, and disabling it outside dev removes the endpoint entirely.
9. **Concept:** default-deny; attack surface reduction; `web.ignoring()` vs `permitAll()`; clickjacking headers.
10. **Test:** `SecurityHardeningWebTest.openDevToolingPaths_areNoLongerPublic`.
11. **Verification — PARTIAL.** Verified: `/h2-console/` and `/api/test/**` now return 401 (and the mutation that
    re-opened `/h2-console/**` failed the test). **Not verified:** that Swagger UI still loads in dev — I couldn't run
    the app. Please open `http://localhost:8080/swagger-ui.html` once. I also removed legacy paths
    (`/v2/api-docs`, `/swagger-resources/**`, `/configuration/**`) on the assumption that springdoc 2.x doesn't use them.
12. **Related risks:** if you deploy with Swagger disabled, API docs disappear (intended); enable with
    `SPRINGDOC_API_DOCS_ENABLED=true` / `SPRINGDOC_SWAGGER_UI_ENABLED=true` and protect it.

---

## Bug 11 — Access-denied returned an empty body

1. **Title:** Inconsistent error responses (401 has JSON, 403 doesn't).
2. **Where:** `auth/WebSecurityConfig.java` — only an `AuthenticationEntryPoint` was configured.
3. **Supposed to do:** consistent, machine-readable errors.
4. **Actually did:** a `USER` requesting `/api/admin/**` got a bare `403` with no body; a missing login got a JSON 401.
5. **Root cause:** Spring Security has *two* handlers — `AuthenticationEntryPoint` (401, "who are you?") and
   `AccessDeniedHandler` (403, "you may not"). Only the first was customised.
6. **How found:** reading the security config's `exceptionHandling` block.
7. **Fix:** an `AccessDeniedHandler` bean returning `{status, error, message, path}` JSON, wired into the chain.
8. **Why it works:** the framework calls that handler whenever an authenticated user is denied.
9. **Concept:** 401 vs 403; the two exception-handling hooks in Spring Security.
10. **Test:** `userRole_isForbiddenFromAdminRoutes_withAJsonBody`, `anonymous_isRejectedFromAdminRoutes`.
11. **Verification: green + mutation-style** (asserts a JSON content type that the default handler doesn't produce);
    I did not run a separate mutation for this one.
12. **Related risks:** `AuthEntryPoint` puts `authException.getMessage()` in both `error` and `message`. Cosmetic.

---

## Bug 12 — JWT filter registered twice

1. **Title:** `AuthTokenFilter` was both a `@Component` and a `@Bean`, and a `Filter` bean.
2. **Where:** `auth/jwt/AuthTokenFilter.java`; `auth/WebSecurityConfig.java`.
3. **Supposed to do:** run once, inside the security filter chain.
4. **Actually did:** Spring Boot auto-registers any `Filter` bean as a *servlet* filter as well as the chain's
   own use of it. `OncePerRequestFilter` prevented double execution, so this was a latent hazard rather than a
   visible failure.
5. **Root cause:** a well-known Spring Boot pitfall — declaring a custom security filter as a bean.
6. **How found:** reading the `@Component` annotation next to the `@Bean authTokenFilter()` method.
7. **Fix:** removed `@Component`; kept the `@Bean`; added a `FilterRegistrationBean` with `setEnabled(false)` so
   Boot doesn't also register it as a servlet filter.
8. **Why it works:** Boot skips auto-registration for a `Filter` that already has a `FilterRegistrationBean`; disabling
   that registration leaves the filter only in the security chain.
9. **Concept:** servlet filter chain vs Spring Security's filter chain; auto-registration of `Filter` beans.
10. **Test:** none dedicated.
11. **Verification — PARTIAL.** The web tests prove the chain still authenticates via the cookie (so nothing broke),
    but **nothing tests that the extra servlet registration is gone.** I'm reporting this as a low-risk hygiene change, not a
    proven fix.
12. **Related risks:** none identified.

---

## Bug 13 — Signup validation rules were never enforced

1. **Title:** `@Size`/`@Email` on `SignupUser` had no effect.
2. **Where:** `AuthController.signupUser(...)`, `SignupUser`.
3. **Supposed to do:** reject passwords under 8 characters, etc.
4. **Actually did:** accepted any password (even one character). The annotations existed but the controller
   parameter had no `@Valid`.
5. **Root cause:** Bean Validation annotations are **declarations**; something must trigger them. `@Valid` on
   the `@RequestBody` parameter is the trigger.
6. **How found:** noticing the DTO had constraints while the controller had no `@Valid` (the project has a
   `MethodArgumentNotValidException` handler waiting for it).
7. **Fix:** `@Valid @RequestBody SignupUser`.
8. **Why it works:** Spring MVC validates before the method runs; failures become a 400 through your existing
   `GlobalExceptionHandler`.
9. **Concept:** declarative validation needs a trigger; validate at the boundary.
10. **Test:** `SecurityHardeningWebTest.signup_enforcesPasswordRules`.
11. **Verification: VERIFIED (green).** Passes now; I did not get a red baseline (context failure, as in Bug 1).
12. **Related risks:** the DTO says `username` min 4, the entity `Users` says min 5 — a 4-character username passes
    the DTO and then fails at persistence. I read this but didn't test what the user sees. `login` has no validation.
    There is **no login rate-limiting or lockout** (not fixed).

---

## Bug 14 — Project doesn't compile on the installed JDK (Lombok)

1. **Title:** Lombok annotation processor doesn't run on JDK 25.
2. **Where:** `pom.xml` (Lombok dependency, compiler config).
3. **Supposed to do:** build.
4. **Actually did:** `mvn compile` on this machine failed with dozens of "cannot find symbol" errors for getters
   (`getStockQuantity()`, `getQuantity()`…), i.e. Lombok-generated code missing.
5. **Root cause:** starting with JDK 23, `javac` no longer runs annotation processors found only on the classpath
   unless told to (`-proc:full`). The pom declares Lombok with `<scope>annotationProcessor</scope>` (not a valid
   Maven scope — Maven warns about it) and has no `annotationProcessorPaths`. A newer JDK (25 here) exposes what
   an older JDK tolerated. Your IDE probably works because it configures processing itself.
6. **How found:** my first `mvn compile`.
7. **Fix: none applied.** I worked around it on the command line with `-Dmaven.compiler.proc=full`.
8. **Why that works:** it re-enables annotation processing so Lombok runs. It's a workaround, not a repair.
9. **Concept:** build reproducibility; toolchain versions; annotation processing; invalid config that "worked by accident".
10. **Test:** n/a.
11. **Verification:** the workaround compiles; the 30 tests run with it.
12. **Suggested fix (not applied — it's your build):** in `maven-compiler-plugin` add
    `<annotationProcessorPaths>` with Lombok, and change the Lombok dependency to `<optional>true</optional>`.
    Also: the pom targets Java 21 but the shell's default is JDK 25; consider a Maven toolchain. Running tests also
    needed Maven to download Surefire (the plugin wasn't in the local cache).

---

## What I did NOT fix (investigated, left alone)

- **The `SELLER` role does nothing:** no `/api/seller/**` route; `/api/user/**` excludes sellers, so `seller1` can't even shop.
- **No token revocation** (Bug 7) and **no rate limiting** on sign-in (Bug 13).
- **Account enumeration** on signup (distinct "username/email exists" messages).
- **No way to grant elevated roles** now that signup is locked (Bug 1).
- **No CORS configuration seen**, so a cross-origin frontend is untested (Bug 3).
- **No schema migrations** (Bug 4).
- **`initiatePayment` can create multiple payments for one order** (Bug 6, unverified).
- **Your existing integration tests** (`SbEcommApplicationTests`, including the concurrency tests) — not run (no Docker).
- **Full endpoint authorization audit** — only the paths named above were checked.

---

## What I Should Learn From These Bugs

### Authentication (who are you?)
- **Bug 3, 7, 8, 9.** Authentication is mostly *plumbing that must fail closed*: a bad, expired, forged, or missing
  credential must always become "anonymous" (401), never an exception or a 500. Bug 9 shows the trap: an incomplete
  `catch` list on attacker-controlled input. Bug 8 shows that "public" is a per-route decision, not per-prefix.

### Authorization (what may you do?)
- **Bugs 1, 2, 6.** These are the three worst bugs and all are the same mistake in different clothes: **the server
  trusted something it should have decided itself** — a role from the request (1), a public caller with signing
  power (2), an id from the URL without checking it belongs to you (6). Rules: privilege comes from the server;
  every object lookup is scoped to the caller; "logged in" is not "allowed".

### Spring Security specifics
- **Bugs 8, 10, 11, 12.** First-match-wins rule order; `permitAll` vs `web.ignoring()`; `AuthenticationEntryPoint`
  (401) vs `AccessDeniedHandler` (403); `Filter` beans get auto-registered as servlet filters.

### JWT and cookies
- **Bugs 3, 5, 7, 9.** A token is a password with an expiry: never log it (5), never expose it to scripts (3),
  make its lifetime and its cookie's agree (7), and understand that *stateless* means you cannot revoke (7).
  `HttpOnly` protects against XSS; `SameSite` against CSRF — different attacks, different flags.

### Configuration and secrets
- **Bugs 2, 4.** Dev conveniences (seed admins, mock payment providers, drop-on-exit schemas) must be *opt-in*,
  and secrets must come from the environment with no default so a mistake fails at startup instead of in production.
  Also: know what your `.gitignore` hides — the config was never in git, which is *why* it was never noticed.

### Database and data access
- **Bug 6.** The safest authorization check lives *in the query* (`findByIdAndUserId`), because a row you never
  load can't leak. Also: derived-query names with several `Long` ids are easy to mis-order.

### Concurrency
- **None of these 14 bugs is a concurrency bug**, so I'm not going to pretend otherwise. Your existing tests cover
  races (idempotency keys, double-capture) but I could not run them (no Docker). Worth noting: Bug 6's
  double-payment risk is a *check-then-act* problem and would need a database constraint or lock, not just a check.

### Error handling
- **Bugs 8, 9, 11.** A client mistake must be a 4xx, never a 5xx (8); catch exception *hierarchies* specific-to-general
  and don't let a catch-all mask a real defect (9); errors should share one shape (11).

### API design
- **Bugs 1, 3, 6, 13.** Don't put security-relevant fields in request DTOs (1); don't return the same secret twice (3);
  hide the existence of other people's resources with 404 (6); declared validation must actually be triggered (13).
  Also: removing a field can break clients — I used `@JsonIgnoreProperties` because your `ObjectMapper` is strict.

### Testing
- **All bugs.** Three lessons from this very exercise:
  1. **A test only proves something if you've seen it fail.** Bugs 5, 6, 9 have a real red→green. For the others
     my baseline was invalid (the test class couldn't start), so I *re-broke each fix on purpose* (mutation testing)
     to prove the tests can fail. That's the honest label "green + mutation".
  2. **Unit tests with mocks prove the code asks the right question; only an integration test proves the database
     answers correctly.** Bug 6 is only unit-tested.
  3. **Tests that depend on gitignored files must skip, not fail, on a fresh clone** (`ConfigProfilesTest`).

### Java / Spring fundamentals
- **Bugs 1, 6, 9, 12, 14.** Lombok is an annotation processor and depends on the compiler flags of your JDK (14);
  positional parameters of the same type are a bug factory (6); exception class hierarchies decide what `catch`
  matches (9); Spring's auto-registration of `Filter` beans (12); `@ConditionalOnProperty` and profiles decide which
  beans *exist* at all (2, 4); a hand-built `new ObjectMapper()` silently replaces Spring Boot's configured one (1).

---

## Files changed

**Production code:** `auth/WebSecurityConfig.java`, `auth/jwt/JwtUtils.java`, `auth/jwt/AuthTokenFilter.java`,
`auth/controllers/AuthController.java`, `auth/payload/request/SignupUser.java`, `gateway/MockGatewayController.java`,
`payment/services/PaymentServicesImpl.java`, `payment/repository/PaymentRepository.java`.
**Config (gitignored):** `src/main/resources/application.properties` (rewritten), `application-dev.properties` (new).
**Repo:** `.gitignore` (+2 lines).
**Tests (new):** `src/test/java/com/ecomm/sb_ecomm/security/` — `SecurityHardeningWebTest` (12), `JwtUtilsTest` (6),
`PaymentOwnershipTest` (5), `MockGatewayGatingTest` (3), `ConfigProfilesTest` (3), `SignupUserJsonTest` (1).
**Not touched:** `pom.xml`, existing tests, the Swagger annotations already in your working tree.
