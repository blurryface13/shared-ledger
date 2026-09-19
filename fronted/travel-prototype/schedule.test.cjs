const {test}=require('node:test');const assert=require('node:assert/strict');const {reorder}=require('./schedule.js');
const a=(id,time,duration=60,extra={})=>({id,time,duration,day:0,...extra});
test('reorder preserves durations and positional gaps without mutating input',()=>{const items=[a('a','09:00'),a('b','11:00',30)];const next=reorder(items,'b','a');assert.deepEqual(next.map(x=>[x.id,x.time,x.duration]),[['b','09:00',30],['a','10:30',60]]);assert.equal(items[0].time,'09:00');});
test('reject changing locked or completed arrangements',()=>{for(const flag of ['locked','done'])assert.throws(()=>reorder([a('a','09:00',60,{[flag]:true}),a('b','10:00')],'b','a'),/解锁|打卡/);});
test('reject missing targets and mixed dates',()=>{assert.throws(()=>reorder([a('a','09:00')],'x','a'));assert.throws(()=>reorder([a('a','09:00'),a('b','10:00',60,{day:1})],'b','a'));});
test('same position leaves schedule intact',()=>{const items=[a('a','09:00')];assert.deepEqual(reorder(items,'a','a'),items);});
