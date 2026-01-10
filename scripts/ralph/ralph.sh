#!/bin/bash
set -e

MAX_ITERATIONS=${1:-10}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Starting Ralph"

test -f "$SCRIPT_DIR/prompt.md"

test -f "$SCRIPT_DIR/prd.json"

test -f "$SCRIPT_DIR/progress.txt"

for i in $(seq 1 $MAX_ITERATIONS); do
  echo "═══ Iteration $i ═══"

  OUTPUT=""
  for attempt in 1 2 3; do
    echo "[$(date +%Y-%m-%dT%H:%M:%S%z)] Running Claude (attempt $attempt/3)..."

    TMP_OUT=$(mktemp)
    set +e
    (cat "$SCRIPT_DIR/prompt.md" | claude -p --dangerously-skip-permissions --fallback-model sonnet) >"$TMP_OUT" 2>&1 &
    CLAUDE_PID=$!
    set -e

    while kill -0 "$CLAUDE_PID" 2>/dev/null; do
      echo "[$(date +%Y-%m-%dT%H:%M:%S%z)] Claude still running..."
      sleep 10
    done

    set +e
    wait "$CLAUDE_PID"
    CLAUDE_STATUS=$?
    set -e

    OUTPUT=$(cat "$TMP_OUT")
    rm -f "$TMP_OUT"

    if [ -n "$OUTPUT" ]; then
      echo "$OUTPUT" | tee /dev/stderr >/dev/null
    fi

    if [ $CLAUDE_STATUS -eq 0 ]; then
      break
    fi

    if echo "$OUTPUT" | grep -qiE "unavailable:|tls: bad record mac"; then
      echo "Transient Claude error (attempt $attempt/3). Retrying..."
      sleep 3
      continue
    fi

    break
  done

  if echo "$OUTPUT" | grep -q "<promise>COMPLETE</promise>"; then
    echo "Done"
    exit 0
  fi

  sleep 2
done

echo "Max iterations reached"
exit 1
