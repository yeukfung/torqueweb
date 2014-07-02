db.counters.insert(
   {
      _id: "key",
      seq: 0
   }
)


function getNextSeq(name) {
   var ret = db.counters.findAndModify(
          {
            query: { _id: name },
            update: { $inc: { seq: 1 } },
            new: true
          }
   );

   return ret.seq;
}