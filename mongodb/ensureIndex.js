db.sessionheaders.ensureIndex({ "eml": 1})

db.sessionlogs.ensureIndex({ "eml": 1, "session": 1})
db.sessionlogs.ensureIndex({ "eml": 1, "session": 1, "time": 1})