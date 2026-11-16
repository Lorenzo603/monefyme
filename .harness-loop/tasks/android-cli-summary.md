# Android CLI Documentation Summary

## Overview

Android CLI is a command-line interface that enables efficient Android development with AI agents. It standardizes core development competencies for agent-first workflows, providing an entry point to official tools, skills, and knowledge.

## Key Features

### Core Capabilities

1. **Project Management**
   - Scaffold new projects from templates (`android create`)
   - Manage Android SDK (`android sdk`)
   - Initialize environment for agents (`android init`)

2. **Build & Deploy**
   - Build Android artifacts (`android build`)
   - Install APKs on devices (`android install`)
   - Run apps on devices/emulators (`android run`)

3. **Device Management**
   - Manage virtual devices (`android emulator`)
   - Mirror and control connected devices (`android device`)
   - Inspect and interact with live device screens (`android screen`, `android input`)

4. **Development Tools**
   - Render Jetpack Compose previews to images (`android preview`)
   - Connect to Android Studio for semantic analysis (`android studio`)
   - Analyze APKs, bundles, and libraries (`android analyze`)

5. **Testing & Profiling**
   - Test apps with Journeys (`android journeys`)
   - Profile apps by recording system traces (`android profiler`)
   - Investigate crashes and ANRs from Firebase Crashlytics (`android signals`)

6. **Documentation**
   - Search and fetch official Android documentation (`android docs`)

## Android Skills

Android skills are AI-optimized instructions that help agents understand and execute Android-specific patterns following best practices.

### Key Benefits
- **Ground with expert knowledge**: Provide Android-specific context to agents
- **Repeatable workflows**: Standardize multi-step Android development tasks
- **Resource bundling**: Include scripts, templates, and documentation
- **Shared expertise**: Package team workflows for unified access

### Available Skills
Skills are housed in the [Android Skills GitHub repository](https://github.com/android/skills/tree/main/devtools/android-cli). Common skills include:
- Migration from XML to Compose
- Upgrading to AGP 9
- Setting up Navigation 3
- Making UI edge-to-edge
- Auditing R8 configuration

### Using Skills
```bash
# List available skills
android skills list

# Install a skill
android skills add skill-name

# Initialize Android CLI (installs android-cli skill)
android init
```

## Agent Plugins

Android CLI is available as official plugins for:
- **Antigravity**: Deeper platform integration with interactive tools
- **Claude Code**: Via marketplace plugin
- **OpenAI Codex**: Via marketplace plugin

Each plugin adds the `android-cli` skill to enable agent workflows.

## Best Practices for Agentic Coding

### 1. Environment Setup
```bash
# Check if installed
which android

# Update to latest version
android update

# Initialize for agents
android init
```

### 2. Configure Android CLI
Create a `.androidrc` file in your home directory to apply flags automatically:
```
--sdk=<path-to-sdk>
```

### 3. Use Skills Effectively
- Skills are automatically activated when agents detect relevant tasks
- In Android Studio, invoke skills manually with `@skill-name`
- Customize skills by renaming them to prevent overwrites during updates

### 4. Journeys for Testing
Journeys leverage AI agents to test apps using natural language instructions:
- Define test scenarios in natural language
- Agents convert instructions into app interactions
- Can be deployed in CI/CD pipelines
- Works with terminal-based agents via Android CLI

### 5. Data Collection
Android CLI collects basic usage metrics:
- Command invocations and options used
- Emulator template names and agent names
- Anonymized stack traces

Disable with `--no-metrics` flag.

## Recommended Workflow

1. **Install and Initialize**
   ```bash
   android update
   android init
   ```

2. **Install Relevant Skills**
   ```bash
   android skills list
   android skills add edge-to-edge
   android skills add migrate-xml-views-to-jetpack-compose
   ```

3. **Use Agent Commands**
   - Create projects: `android create --name=MyApp`
   - Build and run: `android build && android run`
   - Test with Journeys: `android journeys run`

4. **Integrate with IDE**
   - Open generated projects in Android Studio
   - Use visual editors, debuggers, and AI assistance for production

## Command Reference

| Command | Description |
|---------|-------------|
| `android analyze` | Analyze APKs, AABs, JARs, AARs |
| `android build` | Build Android artifacts |
| `android create` | Create new projects from templates |
| `android device` | Mirror and control devices |
| `android emulator` | Manage virtual devices |
| `android install` | Install APKs on devices |
| `android preview` | Render Compose previews |
| `android profiler` | Profile app performance |
| `android run` | Deploy and launch apps |
| `android screen` | Inspect device screens |
| `android sdk` | Manage Android SDK |
| `android skills` | Manage Android CLI skills |
| `android studio` | Connect to Android Studio |

## Resources

- [Android CLI Main Page](https://developer.android.com/tools/agents/android-cli)
- [Command Reference](https://developer.android.com/tools/agents/android-cli/commands)
- [Android Skills Repository](https://github.com/android/skills)
- [Agent Plugins Documentation](https://developer.android.com/tools/agents/android-cli/agent-plugins)
- [Journeys Testing](https://developer.android.com/tools/agents/android-cli/journeys)
