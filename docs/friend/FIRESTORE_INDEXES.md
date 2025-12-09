# Firestore Indexes for Friends Feature

This document describes the composite indexes required for the Friends Feature to function correctly. These indexes must be created in the Firebase Console before deploying the feature.

## Required Composite Indexes

### friendRequests Collection

The following composite indexes are required for the `friendRequests` collection:

#### 1. Incoming Requests Query Index
Used to fetch pending incoming friend requests for a user, sorted by newest first.

| Field | Order |
|-------|-------|
| `toUserId` | Ascending |
| `status` | Ascending |
| `createdAt` | Descending |

**Query Pattern:**
```kotlin
firestore.collection("friendRequests")
    .whereEqualTo("toUserId", userId)
    .whereEqualTo("status", "pending")
    .orderBy("createdAt", Query.Direction.DESCENDING)
```

#### 2. Outgoing Requests Query Index
Used to fetch pending outgoing friend requests for a user, sorted by newest first.

| Field | Order |
|-------|-------|
| `fromUserId` | Ascending |
| `status` | Ascending |
| `createdAt` | Descending |

**Query Pattern:**
```kotlin
firestore.collection("friendRequests")
    .whereEqualTo("fromUserId", userId)
    .whereEqualTo("status", "pending")
    .orderBy("createdAt", Query.Direction.DESCENDING)
```

#### 3. Reciprocal Request Check Index
Used to check if a reciprocal request exists between two users.

| Field | Order |
|-------|-------|
| `fromUserId` | Ascending |
| `toUserId` | Ascending |
| `status` | Ascending |

**Query Pattern:**
```kotlin
firestore.collection("friendRequests")
    .whereEqualTo("fromUserId", targetUserId)
    .whereEqualTo("toUserId", currentUserId)
    .whereEqualTo("status", "pending")
```

## How to Create Indexes in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to **Firestore Database** → **Indexes** tab
4. Click **Create Index**
5. For each index above:
   - Select **Collection ID**: `friendRequests`
   - Add the fields in the order specified
   - Set the **Query scope** to **Collection**
   - Click **Create**

## Alternative: firestore.indexes.json

You can also deploy indexes using the Firebase CLI. Create a `firestore.indexes.json` file:

```json
{
  "indexes": [
    {
      "collectionGroup": "friendRequests",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "toUserId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "friendRequests",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "fromUserId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "friendRequests",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "fromUserId", "order": "ASCENDING" },
        { "fieldPath": "toUserId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}
```

Then deploy with:
```bash
firebase deploy --only firestore:indexes
```

## Notes

- Index creation can take several minutes to complete
- Queries will fail with an error until the required index is built
- Firebase will provide a direct link to create missing indexes in error messages
- Single-field indexes are created automatically by Firestore and don't need manual setup

## Related Requirements

- **Requirement 3.5**: Search query execution with case-insensitive partial matching
- **Requirement 6.3**: Display pending incoming friend requests sorted by newest first
- **Requirement 7.1**: Display pending outgoing friend requests sorted by newest first
