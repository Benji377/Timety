#!/usr/bin/env bash
#
# Fills .github/release_template.md and prints the finished release notes on stdout.
#
# The "What's New" list is built from the closed issues in a milestone ("Next Release" by
# default); the Breaking Changes and Additional Notes sections keep their placeholder text so
# the draft release still has something to edit by hand. Warnings (issues still open, missing
# milestone) go to stderr so the caller can surface them separately.
#
#   ./scripts/release-notes.sh --version 2.3.0
#   ./scripts/release-notes.sh --version 2.3.0 --attestation-url https://github.com/... > notes.md
#
# Options:
#   --version X.Y.Z          Version being released. Required.
#   --previous-tag vX.Y.Z    Tag to diff against. Defaults to the newest v* tag that is not this one.
#   --attestation-url URL    Build provenance link. Defaults to the repo's attestation list.
#   --milestone TITLE        Milestone to read issues from. Defaults to "Next Release".
#   --repo owner/name        Defaults to $GITHUB_REPOSITORY, else whatever `gh` resolves.
#
# Needs an authenticated `gh` (GH_TOKEN on CI).
set -euo pipefail

cd "$(dirname "$0")/.."

VERSION=""
PREVIOUS_TAG=""
ATTESTATION_URL=""
MILESTONE="Next Release"
REPO="${GITHUB_REPOSITORY:-}"

while [ $# -gt 0 ]; do
  case "$1" in
    --version) VERSION="$2"; shift 2 ;;
    --previous-tag) PREVIOUS_TAG="$2"; shift 2 ;;
    --attestation-url) ATTESTATION_URL="$2"; shift 2 ;;
    --milestone) MILESTONE="$2"; shift 2 ;;
    --repo) REPO="$2"; shift 2 ;;
    -h|--help) sed -n '2,25p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "unknown option: $1 (try --help)" >&2; exit 2 ;;
  esac
done

if [ -z "$VERSION" ]; then
  echo "--version is required" >&2
  exit 2
fi

if [ -z "$REPO" ]; then
  REPO=$(gh repo view --json nameWithOwner --jq .nameWithOwner)
fi

if [ -z "$PREVIOUS_TAG" ]; then
  PREVIOUS_TAG=$(git tag --list 'v*' --sort=-v:refname | grep -vFx "v$VERSION" | head -1 || true)
fi

if [ -n "$PREVIOUS_TAG" ]; then
  CHANGELOG_URL="https://github.com/$REPO/compare/$PREVIOUS_TAG...v$VERSION"
else
  CHANGELOG_URL="https://github.com/$REPO/commits/v$VERSION"
fi

if [ -z "$ATTESTATION_URL" ]; then
  ATTESTATION_URL="https://github.com/$REPO/attestations"
fi

# The issues endpoint wants the milestone's number, not its title.
export MILESTONE
milestone_number=$(gh api "repos/$REPO/milestones?state=all&per_page=100" \
  --jq 'map(select(.title == env.MILESTONE)) | .[0].number // empty')

WHATS_NEW=""
if [ -z "$milestone_number" ]; then
  echo "No milestone titled \"$MILESTONE\" found; the What's New section is left as a placeholder." >&2
else
  issues=$(gh api --paginate \
    "repos/$REPO/issues?milestone=$milestone_number&state=all&per_page=100" \
    --jq '.[] | select(.pull_request == null) | [.state, .number, .title] | @tsv')

  # Issue titles are tagged "[FEAT]", "[BUG]" and so on; the leading tag, whatever it says, has no
  # business in the release notes. Only the first one is dropped, so a title that genuinely starts
  # with a bracketed word keeps the rest of it.
  WHATS_NEW=$(printf '%s\n' "$issues" \
    | awk -F'\t' '$1 == "closed" { title = $3; sub(/^\[[^]]*\][ \t]*/, "", title); printf "- %s (#%s)\n", title, $2 }')

  still_open=$(printf '%s\n' "$issues" \
    | awk -F'\t' '$1 == "open" { printf "- %s (#%s)\n", $3, $2 }')
  if [ -n "$still_open" ]; then
    echo "Issues still open in \"$MILESTONE\" (not listed in the notes):" >&2
    echo "$still_open" >&2
  fi
fi

if [ -z "$WHATS_NEW" ]; then
  WHATS_NEW="- List new features, improvements, or bug fixes here."
fi

export VERSION WHATS_NEW CHANGELOG_URL ATTESTATION_URL

# Going through %ENV keeps values with backslashes or & in them from being reinterpreted,
# which a sed-based substitution would not.
perl -pe 's/\{\{(\w+)\}\}/exists $ENV{uc $1} ? $ENV{uc $1} : $&/ge' .github/release_template.md
