# Agent Coordination Protocol

## Decision Process

1. **Propose** - Agent documents proposed change in relevant spec
2. **Impact** - Identify dependent agents/components
3. **Review** - Affected agents review and provide feedback
4. **Approve** - Architecture agent approves if no conflicts
5. **Update** - Update all relevant specifications
6. **Migrate** - If breaking change, provide migration path

## Conflict Resolution

| Type | Resolution |
|------|------------|
| Technical disagreement | Requirements & Architecture Agent decides |
| Priority conflict | Project lead prioritizes |
| Blocking issue | Escalate with impact analysis |

## Sync Points

### Sync Point 1: Rootfs Layout Lock-in
- **When**: After Phase 1, before Phase 2
- **Decision**: Final rootfs directory structure
- **Blocks**: All rootfs-related code

### Sync Point 2: Display Strategy
- **When**: Before Phase 3
- **Decision**: VNC bridge vs. Wayland vs. XServer
- **Blocks**: GUI implementation, input handling

### Sync Point 3: Session API Contract
- **When**: After Session service (B3)
- **Decision**: Public API for `UbuntuSessionManager` and `UbuntuSession`
- **Blocks**: UI integration, testing

## Definition of Done

A task is complete when:
- [ ] Implementation matches interface contracts
- [ ] Integration points verified with dependent components
- [ ] No known regressions

## Agent Dependency Graph

```
Agent 1 (Architecture)
    ↓
    ├── Agent 2 (Android Shell) ←→ Agent 3 (PRoot)
    │       ↓                         ↓
    │       └─────────┬───────────────┘
    │                 ↓
    │            Agent 4 (GUI)
    │
    ├── Agent 5 (Packaging) ←→ Agent 3 (PRoot)
    │
    └── Agent 6 (DevEx) ← All agents
```
