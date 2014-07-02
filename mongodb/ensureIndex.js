db.userprofiles.ensureIndex({
  "id": 1
})
db.races.ensureIndex({
  "id": 1
})
db.racecars.ensureIndex({
  "id": 1
})


db.sessionheaders.ensureIndex({
  "eml": 1
})


db.sessionlogs.ensureIndex({
  "eml": 1,
  "session": 1
})
db.sessionlogs.ensureIndex({
  "eml": 1,
  "session": 1,
  "time": 1
})