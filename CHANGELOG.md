# SQMan Changelog

## Latest Updates (2026-02-17)

### ✅ Simplified Single-Instance Model

**Removed multi-instance complexity:**
- Removed custom port option (`-p` / `--port`) - always uses port 9000
- Removed console/foreground mode option (`-c` / `--console`) - always runs in background
- Only one instance can run at a time
- Removed `RunningInstanceRegistry` - uses PID files directly for process tracking
- Simplified `run` command - checks if any instance is running before starting
- Simplified `stop` command - automatically finds and stops the running instance
- Simplified `delete` command - uses PID files to check running status

**Benefits:**
- Cleaner, simpler API
- Easier to understand and use
- Reduced complexity in codebase
- More reliable process tracking using native .pid files

### ✅ Delete Command

**New command to delete local SonarQube installations:**
- `sqman delete [version|index]` - Delete a SonarQube instance
- Interactive mode when no version specified - shows list and prompts for selection
- Supports version strings, partial matches, and numeric indices (like run/stop commands)
- Safety features:
  - Prevents deletion of running instances (unless `--force` is used)
  - Prompts for confirmation before deletion (unless `--yes` is used)
  - Shows instance location and status before deletion
- Flags:
  - `-f, --force` - Force deletion even if instance is running (stops it first)
  - `-y, --yes` - Skip confirmation prompt

**Examples:**
```bash
sqman delete                    # Interactive mode
sqman delete 26.2.0.119303      # Delete specific version (with confirmation)
sqman delete 1 -y               # Delete first instance without confirmation
sqman delete 10.7 -f            # Force delete (stops if running)
```

## Updates (2026-02-16)

### ✅ Version Service Improvements

**Changed version discovery to use GitHub Releases API:**
- Now fetches from public `SonarSource/sonarqube` releases
- Supports both old versioning (10.7.0.96327) and new year-based versioning (26.2.0.119303, 25.12.0, etc.)
- **GITHUB_TOKEN is now optional** (only needed for higher API rate limits)
- Pagination support - fetches up to 300 releases across 3 pages
- Proper version sorting works with year-based versions (26.x appears before 25.x and 10.x)

**Simplified Edition Model:**
- Removed per-edition version listing
- All versions are now shown in a single list
- Clear distinction between **SQCB** (SonarQube Community Build) and **SQS** (SonarQube Server)
- Edition (Community vs Server features) is determined by license file, not version
- No `-e` flag on `versions` command - just list all versions

**Example output:**
```
sqman versions -l 15

Available SonarQube versions (showing 15 of 70):

Note: All versions support both SQCB (Community Build) and SQS (Server).
      Edition is determined by your license file.

  1. 26.2.0.119303
  2. 26.1.0.118079
  3. 25.12.0.117093
  4. 25.11.0.114957
  5. 25.10.0.114319
  6. 25.9.0.112764
  7. 25.8.0.112029
  8. 25.7.0.110598
  9. 25.6.0.109173
  10. 25.5.0.107428
  ...

Download with:
  sqman download <version> -e community     # for SQCB
  sqman download <version> -e developer     # for SQS (Developer)
  sqman download <version> -e enterprise    # for SQS (Enterprise)
  sqman download <version> -e datacenter    # for SQS (DataCenter)
```

---

### ✅ Run Command (Fully Implemented)

**Features:**
- Runs in **background mode by default** (no longer blocks terminal)
- Platform detection (Windows, Linux, macOS)
- Custom port configuration via `-p` flag
- Console/foreground mode available with `-c` flag
- Automatic PID file detection from `bin/{platform}/SonarQube.pid`
- Startup monitoring with PID file verification
- Partial version matching support (e.g., `sqman run 10.7` finds `10.7.0.96327`)

**Usage:**
```bash
# Run in background (default)
sqman run 10.7.0.96327
sqman run 26.2.0.119303

# Run with custom port
sqman run 10.7 -p 9001

# Run in console/foreground mode
sqman run 10.7 --console

# Partial version matching
sqman run 10.7    # Finds exact match like 10.7.0.96327
```

---

### ✅ Stop Command (Fully Implemented)

**Features:**
- Reads PID from correct location: `bin/{platform}/SonarQube.pid`
- Graceful shutdown by default (uses sonar.sh stop on Unix, taskkill on Windows)
- Force kill option with `-f` flag
- Automatic PID file cleanup
- Platform-specific process management
- Partial version matching support

**Usage:**
```bash
# Stop gracefully
sqman stop 10.7.0.96327
sqman stop 26.2

# Force stop
sqman stop 10.7 --force
```

---

### ✅ Download Command Improvements

**Fixed:**
- Corrected default directory handling (now properly uses `~/.sqman`)
- Fixed ZIP extraction when folder already has correct name
- Better error messages for extraction failures

**Features:**
- Downloads from `binaries.sonarsource.com`
- Progress bar with download percentage and size
- Automatic extraction and cleanup
- Supports all editions
- Smart version naming (old "sonar-" vs new "sonarqube-" prefix)

---

### ✅ List Command (Fully Implemented)

**Features:**
- Lists all installed instances from `~/.sqman/`
- Shows installation location
- Helpful suggestions when no instances found

**Usage:**
```bash
sqman list
```

---

## Implementation Status

| Command | Status | Notes |
|---------|--------|-------|
| `versions` | ✅ Complete | Fetches from public releases, supports year-based versions |
| `download` | ✅ Complete | Downloads and extracts all editions |
| `list` | ✅ Complete | Lists installed instances |
| `run` | ✅ Complete | Starts instances in background by default |
| `stop` | ✅ Complete | Graceful and force stop options |
| `config` | ⚠️ Stub | Not yet implemented |

---

## Technical Details

### Version Comparison Algorithm
- Correctly handles year-based versioning (26.x > 25.x > 10.x)
- Part-by-part numeric comparison
- Supports versions with 3 or 4 parts (X.Y.Z or X.Y.Z.BUILD)

### Platform Support
- **macOS**: `bin/macosx-universal-64/sonar.sh`
- **Linux**: `bin/linux-x86-64/sonar.sh`
- **Windows**: `bin/windows-x86-64/StartSonar.bat`

### PID File Location
- Correctly reads from platform-specific directory
- Example: `~/.sqman/sonarqube-10.7.0.96327/bin/macosx-universal-64/SonarQube.pid`

### Process Management
- **Unix/Mac**: Uses `sonar.sh stop` for graceful shutdown, `kill` for force
- **Windows**: Uses `taskkill` with optional `/F` flag

---

## Breaking Changes

### GITHUB_TOKEN
- **Before**: Required for all operations
- **After**: Optional (only recommended for higher rate limits)

### Run Command Default Mode
- **Before**: Console/foreground mode (blocked terminal)
- **After**: Background/detached mode (returns immediately)
- Use `--console` flag to get old behavior

### Version Source
- **Before**: Private `sonar-enterprise` repository tags
- **After**: Public `sonarqube` repository releases
- **Impact**: Now includes year-based versions (26.x, 25.x, etc.)

---

## Migration Guide

### For users of old Python version

**Version listing:**
```bash
# Old Python version
sqman list --limit 10

# New Java version
sqman versions -l 10
```

**Running instances:**
```bash
# Old Python version (foreground by default)
sqman run 10.7.0.96327

# New Java version (background by default)
sqman run 10.7.0.96327

# New Java version (foreground mode)
sqman run 10.7.0.96327 --console
```

**Download and run workflow:**
```bash
# Download latest
sqman download latest

# List installed
sqman list

# Run in background
sqman run 10.7

# Stop when done
sqman stop 10.7
```
