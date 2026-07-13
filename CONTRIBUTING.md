# Contributing to TradeForge MVP

Thank you for your interest in contributing! Please follow these guidelines to keep the codebase clean and maintainable.

---

## Workflow

1. **Create a GitHub Issue** for the work you plan to do.
2. **Create a short-lived branch** from `main` following naming conventions below.
3. **Implement only the issue scope** — one concern per branch.
4. **Add or update tests** for every change.
5. **Update documentation** when the public API or behaviour changes.
6. **Open a Pull Request** against `main`.
7. **Merge only after all CI checks pass** and at least one review approval.
8. **Delete the merged branch**.

---

## Branch Naming

| Type          | Pattern                            | Example                        |
|---------------|------------------------------------|--------------------------------|
| Feature       | `feature/<short-description>`      | `feature/order-aggregate`      |
| Bug fix       | `fix/<short-description>`          | `fix/partial-fill-rounding`    |
| Chore / Setup | `chore/<short-description>`        | `chore/backend-foundation`     |
| Documentation | `docs/<short-description>`         | `docs/modular-monolith-adr`    |
| CI/CD         | `ci/<short-description>`           | `ci/backend-build-workflow`    |

---

## Commit Message Format

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary>

[optional body]
[optional footer]
```

**Types:** `feat`, `fix`, `chore`, `docs`, `test`, `ci`, `refactor`, `perf`, `style`

**Examples:**

```
feat(order): add order aggregate with state machine
fix(matching): prevent negative remaining quantity
chore: add PostgreSQL Docker service
docs: document modular monolith ADR
```

---

## Definition of Done

Every PR must satisfy:

- [ ] Acceptance criteria from the issue are satisfied.
- [ ] Code compiles and runs.
- [ ] Relevant tests are included.
- [ ] All existing tests pass.
- [ ] No secrets or credentials are committed.
- [ ] Errors are handled with clear messages.
- [ ] Logs contain useful context (correlation IDs where applicable).
- [ ] Documentation is updated if the public API changed.
- [ ] All CI checks pass.
- [ ] The feature works through the intended API or UI.

---

## Code Style

- **Java:** Follow standard Java naming conventions. Use `final` for fields where possible.
- **No magic strings:** Define constants or use enums.
- **No hardcoded credentials:** All secrets via environment variables.
- **Lombok:** Optional — prefer explicit code for domain objects.
- **Package structure:** `com.tradeforge.<module>.<layer>` (e.g., `com.tradeforge.order.domain`).

---

## Testing Requirements

- Unit tests must not hit the database or external services.
- Integration tests must use **Testcontainers** for PostgreSQL.
- Aim for high coverage of domain logic and state transitions.
- Name tests using `methodName_stateUnderTest_expectedBehaviour`.

---

## Security Rules

- Never commit `.env`, credentials, API keys, or JWT secrets.
- Use `.env.example` for configuration templates.
- Stack traces must never be returned to API clients.
- All endpoints must require authentication unless explicitly public.

---

## Questions?

Open a GitHub Issue with the label `question` or `documentation`.
