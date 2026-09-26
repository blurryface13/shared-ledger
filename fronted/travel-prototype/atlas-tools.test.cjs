const {test}=require('node:test');
const assert=require('node:assert/strict');
const T=require('./atlas-tools.js');

test('old GCJ-02 stops and new WGS84 POIs meet at the same map position',()=>{
  const converted=T.toWgs(116.404,39.915);
  assert.ok(Math.abs(converted[0]-39.9136)<.0002);
  assert.ok(Math.abs(converted[1]-116.3978)<.0002);
  assert.deepEqual(T.toWgs(116.3978,39.9136,'WGS84'),[39.9136,116.3978]);
  assert.ok(T.distance({longitude:116.404,latitude:39.915},{longitude:116.3978,latitude:39.9136,coordinateSystem:'WGS84'})<30);
  assert.deepEqual(T.toWgs(2.3522,48.8566),[48.8566,2.3522]);
});

test('day filters and weather refuse a mismatched date',()=>{
  const items=[{id:'b',day:1,time:'10:00'},{id:'a',day:0,time:'13:00'},{id:'c',day:0,time:'09:00'}];
  assert.deepEqual(T.ordered(items,0).map(x=>x.id),['c','a']);
  assert.deepEqual(T.ordered(items,-1).map(x=>x.id),['c','a','b']);
  assert.equal(T.dateAt('2026-09-26',2),'2026-09-28');
  const data={daily:{time:['2026-09-26'],temperature_2m_min:[17.4],temperature_2m_max:[25.5],precipitation_probability_max:[40]}};
  assert.deepEqual(T.forecast(data,'2026-09-26'),{low:17,high:26,rain:40,code:undefined});
  assert.equal(T.forecast(data,'2026-09-27'),null);
});
