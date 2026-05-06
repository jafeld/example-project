# Root Cause Analyzer

Simple root cause analysis project for dynamic IP network failures.

The project models a network as a graph of nodes and links, processes events, and attempts to identify likely root causes.

---

# Requirements

* Java 18+
* IntelliJ IDEA (recommended)

---

# Setup

Clone the project and open it in IntelliJ IDEA.

Gradle dependencies should be imported automatically.

---

# Running

## IntelliJ

Run one of the included run configurations:

* `Main`
* `Run Tests`

## Command Line

Run the application:

```bash
./gradlew run
```

Run tests:

```bash
./gradlew test
```

---


# Technologies

* Kotlin 2.2
* Gradle
* H2 Database
* Kotest
