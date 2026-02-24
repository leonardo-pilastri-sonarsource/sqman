# SonarQube Download URL Patterns

## Community Edition (SQCB)

**Base URL:** `https://binaries.sonarsource.com/Distribution/sonarqube/`

**Filename Pattern:**
- Old versions (< 4.0): `sonar-{version}.zip`
- Modern versions: `sonarqube-{version}.zip`

**Examples:**
```
https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-26.2.0.119303.zip
https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-25.12.0.117093.zip
https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-10.7.0.96327.zip
https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-9.9.0.65466.zip
```

---

## Commercial Editions (SQS)

### Developer Edition

**Base URL:** `https://binaries.sonarsource.com/CommercialDistribution/sonarqube-developer/`

**Filename Pattern:**
- Year-based versions (24+): `sonarqube-developer-20{YY}.{M}.{P}.{BUILD}.zip`
- Old versions: `sonarqube-developer-{version}.zip`

**Version Conversion:**
- `26.2.0.119303` → `2026.2.0.119303` (add "20" prefix)
- `25.12.0.117093` → `2025.12.0.117093` (add "20" prefix)
- `10.7.0.96327` → `10.7.0.96327` (no conversion)

**Examples:**
```
https://binaries.sonarsource.com/CommercialDistribution/sonarqube-developer/sonarqube-developer-2026.2.0.119303.zip
https://binaries.sonarsource.com/CommercialDistribution/sonarqube-developer/sonarqube-developer-2025.12.0.117093.zip
https://binaries.sonarsource.com/CommercialDistribution/sonarqube-developer/sonarqube-developer-10.7.0.96327.zip
```

### Enterprise Edition

**Base URL:** `https://binaries.sonarsource.com/CommercialDistribution/sonarqube-enterprise/`

**Filename Pattern:**
- Year-based versions (24+): `sonarqube-enterprise-20{YY}.{M}.{P}.{BUILD}.zip`
- Old versions: `sonarqube-enterprise-{version}.zip`

**Examples:**
```
https://binaries.sonarsource.com/CommercialDistribution/sonarqube-enterprise/sonarqube-enterprise-2026.2.0.119303.zip
https://binaries.sonarsource.com/CommercialDistribution/sonarqube-enterprise/sonarqube-enterprise-2025.12.0.117093.zip
https://binaries.sonarsource.com/CommercialDistribution/sonarqube-enterprise/sonarqube-enterprise-10.7.0.96327.zip
```

### DataCenter Edition

**Base URL:** `https://binaries.sonarsource.com/CommercialDistribution/sonarqube-datacenter/`

**Filename Pattern:**
- Year-based versions (24+): `sonarqube-datacenter-20{YY}.{M}.{P}.{BUILD}.zip`
- Old versions: `sonarqube-datacenter-{version}.zip`

**Examples:**
```
https://binaries.sonarsource.com/CommercialDistribution/sonarqube-datacenter/sonarqube-datacenter-2026.2.0.119303.zip
https://binaries.sonarsource.com/CommercialDistribution/sonarqube-datacenter/sonarqube-datacenter-2025.12.0.117093.zip
https://binaries.sonarsource.com/CommercialDistribution/sonarqube-datacenter/sonarqube-datacenter-10.7.0.96327.zip
```

---

## Implementation Details

### Version Detection

The code automatically detects year-based versions using the major version number:
- Major version 24-99 → Year-based (e.g., 26 = 2026, 25 = 2025)
- Major version < 24 → Regular version (e.g., 10, 9, 8)

### Filename Building Logic

```java
// Community edition examples:
"26.2.0.119303" → "sonarqube-26.2.0.119303.zip"
"10.7.0.96327"  → "sonarqube-10.7.0.96327.zip"

// Developer edition examples:
"26.2.0.119303" → "sonarqube-developer-2026.2.0.119303.zip"
"10.7.0.96327"  → "sonarqube-developer-10.7.0.96327.zip"
```

### Key Methods

- `buildFileName(version, edition)` - Builds the complete ZIP filename
- `isYearBasedVersion(majorVersion)` - Checks if version is year-based (24-99)
- `convertToCommercialVersionFormat(version)` - Converts 26.x → 2026.x for commercial editions

---

## Testing Examples

```bash
# Community edition - no version conversion
sqman download 26.2.0.119303 -e community
# Downloads: sonarqube-26.2.0.119303.zip

# Developer edition - version conversion for year-based versions
sqman download 26.2.0.119303 -e developer
# Downloads: sonarqube-developer-2026.2.0.119303.zip

# Developer edition - no conversion for old versions
sqman download 10.7.0.96327 -e developer
# Downloads: sonarqube-developer-10.7.0.96327.zip

# Enterprise edition - version conversion
sqman download 25.12.0.117093 -e enterprise
# Downloads: sonarqube-enterprise-2025.12.0.117093.zip
```
