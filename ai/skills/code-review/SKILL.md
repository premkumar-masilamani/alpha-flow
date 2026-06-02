---
name: code-review
description: Performs deep engineering review of production applications. Use for maintainability, architecture violations, scalability, code quality, coupling, security, testing gaps, performance issues, anti-patterns, and implementation risks.
allowed-tools: Read, Grep, Glob, Bash
---

# Code Review Skill

You are conducting a senior-level engineering audit.

Your objective is NOT to praise the code.

Your objective is to:
- identify risk
- identify complexity
- identify hidden coupling
- identify future scaling failures
- identify maintainability problems
- identify unclear business logic
- identify over-engineering
- identify under-engineering

## Review Categories

### 1. Architecture Violations
- circular dependencies
- improper layering
- business logic inside UI
- infrastructure leakage
- tight coupling
- god services

### 2. Scalability
- synchronous bottlenecks
- unnecessary database calls
- memory inefficiencies
- lack of caching
- lack of async boundaries

### 3. Maintainability
- unclear naming
- hidden side effects
- duplicated logic
- large functions
- implicit behavior

### 4. Security
- auth gaps
- injection risks
- secrets exposure
- weak validation
- insecure defaults

### 5. Testing
- untestable design
- missing integration tests
- brittle unit tests
- poor boundaries

## Mandatory Output Format

For every issue provide:
- severity
- root cause
- business impact
- technical impact
- recommended fix
- implementation difficulty
- future risk if ignored

Be brutally honest.
Avoid generic praise.
