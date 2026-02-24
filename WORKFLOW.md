# SQMan Workflow Guide

## The Simplified Workflow

SQMan now features an **intelligent, interactive workflow** that makes managing SonarQube instances effortless.

### Basic Workflow

```bash
# Download an instance
sqman download latest

# Run it (interactive)
sqman run

# Stop it (automatic)
sqman stop
```

That's it! No need to remember version numbers or type long commands.

---

## How It Works

### 1. Running Instances - `sqman run`

**Without arguments** (Interactive Mode):
```bash
$ sqman run

Installed SonarQube instances:

  1. 10.7.0.96327
  2. 2026.1.0.119033
  3. 26.1.0.118079

Select instance to run (1-3, or 0 to cancel): 1
```

Just type the number and press Enter!

**With arguments** (Direct Mode):
```bash
sqman run 1                    # By index
sqman run 10.7.0.96327         # By version
sqman run 10.7                 # Partial match
sqman run 26.2 -p 9001         # Custom port
```

### 2. Stopping Instances - `sqman stop`

**Without arguments** (Smart Stop):

**Scenario A: Only 1 instance running**
```bash
$ sqman stop
Stopping the running instance: 10.7.0.96327
```
Automatically stops it - no questions asked!

**Scenario B: Multiple instances running**
```bash
$ sqman stop

Running SonarQube instances:

  1. 10.7.0.96327 (port 9000)
  2. 2026.1.0.119033 (port 9001)

Select instance to stop (1-2, or 0 to cancel): 1
```
Shows what's running and asks which one to stop.

**Scenario C: Nothing running**
```bash
$ sqman stop
No instances are currently running.
```
Friendly message - no errors!

**With arguments** (Direct Mode):
```bash
sqman stop 1                   # By index from list
sqman stop 10.7                # By version
sqman stop 2 --force           # Force kill
```

---

## Running Instance Tracking

SQMan maintains a registry of running instances in `~/.sqman/running.txt`.

**Registry Format:**
```
10.7.0.96327:9000
2026.1.0.119033:9001
26.1.0.118079:9000
```

**Features:**
- Automatically updated when instances start/stop
- Tracks version and port for each instance
- Used by `sqman stop` for smart behavior
- Enables running multiple instances on different ports

---

## Common Workflows

### Workflow 1: Quick Test

```bash
# Download and run latest
sqman download latest
sqman run                      # Select from list
sqman stop                     # Auto-stop when done
```

### Workflow 2: Multiple Versions

```bash
# Download different versions
sqman download 26.2.0.119303 -e community
sqman download 2026.1.0.119033 -e developer

# Run first on port 9000
sqman run 1

# Run second on port 9001
sqman run 2 -p 9001

# List what's running
sqman list

# Stop specific one
sqman stop                     # Interactive selection
```

### Workflow 3: Developer Testing

```bash
# Run specific version in console mode for debugging
sqman run 10.7 --console

# In another terminal, stop it
sqman stop
```

### Workflow 4: Quick Switch

```bash
# Currently running 10.7
sqman stop                     # Quick stop

# Start different version
sqman run                      # Select another from list
```

---

## Index-Based Operations

All list-based commands support index shortcuts:

**List shows indices:**
```bash
$ sqman list

Installed SonarQube instances:
Location: /Users/leonardo.pilastri/.sqman

Found 3 instance(s):

  1. 10.7.0.96327
  2. 2026.1.0.119033
  3. 26.1.0.118079
```

**Use indices anywhere:**
```bash
sqman run 1                    # Run first
sqman run 2 -p 9001            # Run second on port 9001
sqman stop 3                   # Stop third
```

---

## Error Handling

**No instances installed:**
```bash
$ sqman run
No instances installed.

Download an instance with:
  sqman download latest
```

**Invalid selection:**
```bash
$ sqman run
...
Select instance to run (1-3, or 0 to cancel): 99
Invalid selection.
```

**Index out of range:**
```bash
$ sqman run 99
Error: Index 99 out of range (max: 3)
```

**No instances running:**
```bash
$ sqman stop
No instances are currently running.
```

---

## Tips & Tricks

### 1. Press 0 to Cancel
In any interactive prompt, press `0` to cancel:
```bash
Select instance to run (1-3, or 0 to cancel): 0
Cancelled.
```

### 2. Use Partial Versions
Don't type the full version:
```bash
sqman run 10.7         # Finds 10.7.0.96327
sqman stop 26          # Finds 26.1.0.118079
```

### 3. Background by Default
Instances run in background mode by default, so your terminal is free:
```bash
sqman run              # Runs in background
# Terminal is immediately available for other commands
sqman stop             # Stop from same or different terminal
```

### 4. Console Mode for Debugging
Use `--console` to see logs in real-time:
```bash
sqman run 1 --console  # Press Ctrl+C to stop
```

### 5. Multiple Instances
Run multiple versions on different ports:
```bash
sqman run 1            # Runs on default port 9000
sqman run 2 -p 9001    # Runs on port 9001
sqman run 3 -p 9002    # Runs on port 9002
```

---

## Command Summary

| Command | Arguments | Behavior |
|---------|-----------|----------|
| `sqman run` | None | Interactive: shows list, prompts for selection |
| `sqman run 1` | Index | Runs instance at index 1 |
| `sqman run 10.7` | Version | Runs matching version |
| `sqman stop` | None | Smart: auto-stop if 1 running, else prompts |
| `sqman stop 1` | Index | Stops instance at index 1 |
| `sqman stop 10.7` | Version | Stops matching version |
| `sqman list` | None | Shows all installed instances with indices |

---

## File Locations

```
~/.sqman/
├── running.txt              # Registry of running instances
├── sonarqube-10.7.0.96327/  # Installed instance
├── sonarqube-26.2.0.119303/ # Another installed instance
└── ...
```

---

## Next Steps

Now that you understand the workflow, check out:
- `SQCB_vs_SQS.md` - Understanding Community Build vs Server versions
- `DOWNLOAD_URLS.md` - How downloads work for different editions
- `README.md` - Full command reference
