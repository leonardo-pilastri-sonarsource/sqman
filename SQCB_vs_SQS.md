# SQCB vs SQS: Understanding SonarQube Editions

## Overview

SonarQube has two main distribution types with **different build numbers**:

- **SQCB** (SonarQube Community Build) - Open source edition
- **SQS** (SonarQube Server) - Commercial editions (Developer, Enterprise, DataCenter)

## Key Differences

### Build Numbers

**SQCB and SQS have DIFFERENT build numbers for the same release!**

Example:
```
Latest SQCB: 26.2.0.119303
Latest SQS:  2026.1.0.119033
```

Notice:
- Different major/minor versions
- Different build numbers (119303 vs 119033)
- SQS uses full year format (2026) while SQCB uses short year (26)

### Version Tags in GitHub

In the private `sonar-enterprise` repository, versions are tagged as:

```
sqcb-26.2.0.119303      # Community Build
sqs-2026.1.0.119033     # Server (Commercial)
```

### Download URLs

**SQCB (Community Edition):**
```
URL: https://binaries.sonarsource.com/Distribution/sonarqube/
File: sonarqube-26.2.0.119303.zip

Command:
sqman download 26.2.0.119303 -e community
```

**SQS (Commercial Editions):**
```
Developer:
  URL: https://binaries.sonarsource.com/CommercialDistribution/sonarqube-developer/
  File: sonarqube-developer-2026.1.0.119033.zip
  Command: sqman download 2026.1.0.119033 -e developer

Enterprise:
  URL: https://binaries.sonarsource.com/CommercialDistribution/sonarqube-enterprise/
  File: sonarqube-enterprise-2026.1.0.119033.zip
  Command: sqman download 2026.1.0.119033 -e enterprise

DataCenter:
  URL: https://binaries.sonarsource.com/CommercialDistribution/sonarqube-datacenter/
  File: sonarqube-datacenter-2026.1.0.119033.zip
  Command: sqman download 2026.1.0.119033 -e datacenter
```

## Version Listing

### Show All Versions

```bash
sqman versions -l 20
```

Output:
```
Available SonarQube versions (showing 20 of 38):

    1. [SQS]   2026.1.0.119033
    2. [SQS]   2025.6.1.117629
    ...
   19. [SQCB]  26.2.0.119303
   20. [SQCB]  26.2.0.118776
```

### Filter by Type

```bash
# Show only Community Build versions
sqman versions -t sqcb -l 10

# Show only Server (commercial) versions
sqman versions -t sqs -l 10
```

## Download Workflow

### For Community Edition (SQCB)

1. List SQCB versions:
   ```bash
   sqman versions -t sqcb -l 5
   ```

2. Download a specific SQCB version:
   ```bash
   sqman download 26.2.0.119303 -e community
   ```

3. Or download latest SQCB:
   ```bash
   sqman download latest -e community
   ```

### For Commercial Editions (SQS)

1. List SQS versions:
   ```bash
   sqman versions -t sqs -l 5
   ```

2. Download a specific SQS version:
   ```bash
   sqman download 2026.1.0.119033 -e developer
   sqman download 2026.1.0.119033 -e enterprise
   sqman download 2026.1.0.119033 -e datacenter
   ```

3. Or download latest SQS:
   ```bash
   sqman download latest -e developer
   sqman download latest -e enterprise
   sqman download latest -e datacenter
   ```

## Important Notes

1. **Always use the correct build number for your edition:**
   - SQCB versions for community edition
   - SQS versions for commercial editions

2. **Build numbers are NOT interchangeable:**
   - You cannot use a SQCB build number to download commercial editions
   - You cannot use an SQS build number to download community edition

3. **Edition features are determined by license:**
   - All SQS builds (Developer, Enterprise, DataCenter) use the same binaries
   - Your license file determines which features are available
   - Community edition has no license file

4. **The `latest` keyword automatically selects the right build:**
   - `sqman download latest -e community` → uses latest SQCB build
   - `sqman download latest -e developer` → uses latest SQS build

## Version Format Conversion

For commercial editions with year-based versioning, the filename format changes:

| Version Input | Edition | Filename |
|---------------|---------|----------|
| 26.2.0.119303 | community | `sonarqube-26.2.0.119303.zip` |
| 2026.1.0.119033 | developer | `sonarqube-developer-2026.1.0.119033.zip` |
| 2025.6.1.117629 | enterprise | `sonarqube-enterprise-2025.6.1.117629.zip` |

Note: SQS already uses full year format (2026, 2025), so no conversion needed.
