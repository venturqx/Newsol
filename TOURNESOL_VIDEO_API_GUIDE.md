# Tournesol Video Info API Guide

How to fetch all public information about a video from the Tournesol API, in any language.

## Base URL

```
https://api.tournesol.app
```

No authentication is required for the endpoints described here.

## Video UID format

Videos are identified by a UID in the format `yt:<youtube_video_id>`, e.g. `yt:IXR9PByA9SY`.

The UID must be URL-encoded when used in paths: `yt%3AIXR9PByA9SY` (the `:` becomes `%3A`).

## Endpoint 1: Entity details + collective rating

```
GET /polls/videos/entities/{uid}
```

### Example request

```
GET https://api.tournesol.app/polls/videos/entities/yt%3AIXR9PByA9SY
```

### Response structure

```json
{
  "entity": {
    "uid": "yt:IXR9PByA9SY",
    "type": "video",
    "metadata": {
      "name": "Video title",
      "uploader": "Channel name",
      "duration": 414,
      "language": "en",
      "publication_date": "2025-03-10",
      "views": 1234567,
      "description": "...",
      "video_id": "IXR9PByA9SY"
    }
  },
  "entity_contexts": [],
  "collective_rating": {
    "tournesol_score": 56.68,
    "n_comparisons": 113,
    "n_contributors": 30,
    "unsafe": {
      "status": false,
      "reasons": []
    },
    "criteria_scores": [
      {
        "criteria": "largely_recommended",
        "score": 56.7,
        "uncertainty": 0.0,
        "deviation": 0.0
      },
      {
        "criteria": "reliability",
        "score": 22.7,
        "uncertainty": 0.0,
        "deviation": 0.0
      }
    ]
  },
  "recommendation_metadata": {
    "total_score": 56.68
  }
}
```

### Key fields

| Field | Type | Description |
|-------|------|-------------|
| `collective_rating.tournesol_score` | float | Main aggregated score |
| `collective_rating.n_comparisons` | int | Total number of pairwise comparisons |
| `collective_rating.n_contributors` | int | Number of distinct contributors |
| `collective_rating.criteria_scores` | array | Score per criterion (see below) |
| `collective_rating.unsafe.status` | bool | Whether the video is flagged as unsafe |
| `entity.metadata.name` | string | Video title |
| `entity.metadata.uploader` | string | Channel name |
| `entity.metadata.duration` | int | Duration in seconds |

### Criteria scores

Each entry in `criteria_scores` has:

| Field | Type | Description |
|-------|------|-------------|
| `criteria` | string | Criterion identifier (see list below) |
| `score` | float | Aggregated score, range roughly -100 to 100 |
| `uncertainty` | float | Score uncertainty |
| `deviation` | float | Polarization measure among contributors |

### Deriving most/least rated criterion

Sort `criteria_scores` by `score`. The highest is the most rated, the lowest is the least rated.

## Endpoint 2: Criteria score distributions

```
GET /polls/videos/entities/{uid}/criteria_scores_distributions
```

This returns histograms showing how individual contributors scored the video on each criterion.

### Example request

```
GET https://api.tournesol.app/polls/videos/entities/yt%3AIXR9PByA9SY/criteria_scores_distributions
```

### Response structure

```json
{
  "uid": "yt:IXR9PByA9SY",
  "type": "video",
  "metadata": { ... },
  "criteria_scores_distributions": [
    {
      "criteria": "reliability",
      "distribution": [0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 2, 3, 4, 1, 0, 0, 0, 0],
      "bins": [-100, -90, -80, -70, -60, -50, -40, -30, -20, -10, 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100]
    }
  ]
}
```

### Histogram format

Each criterion distribution has:

| Field | Type | Description |
|-------|------|-------------|
| `criteria` | string | Criterion identifier |
| `distribution` | array of 20 floats | Number of contributors whose score falls in each bin |
| `bins` | array of 21 floats | Bin boundaries, from -100 to 100 in steps of 10 |

The i-th bin covers scores in `[bins[i], bins[i+1])`. For example, `distribution[14]` counts contributors whose score is in `[40, 50)`.

The sum of `distribution` gives the total number of public contributor ratings for that criterion.

## List of criteria (for the "videos" poll)

| Identifier | Meaning |
|------------|---------|
| `largely_recommended` | Should be largely recommended (this is the main criterion used for `tournesol_score`) |
| `reliability` | Reliable and not misleading |
| `importance` | Important and actionable |
| `engaging` | Engaging and thought-provoking |
| `pedagogy` | Clear and pedagogical |
| `layman_friendly` | Accessible to non-experts |
| `entertaining_relaxing` | Entertaining and relaxing |
| `diversity_inclusion` | Promotes diversity and inclusion |
| `backfire_risk` | Resilient to backfire risks |
| `better_habits` | Encourages better habits |

## Implementation checklist

1. **HTTP GET** to `/polls/videos/entities/{url_encoded_uid}` — parse JSON response
2. Extract `tournesol_score`, `n_comparisons`, `n_contributors` from `collective_rating`
3. Extract and sort `criteria_scores` to find most/least rated criteria
4. **HTTP GET** to `/polls/videos/entities/{url_encoded_uid}/criteria_scores_distributions` — parse JSON response
5. Iterate `criteria_scores_distributions` to display or process histograms

No auth headers, API keys, or rate-limit tokens are needed. Standard HTTP with JSON parsing is all that's required.
