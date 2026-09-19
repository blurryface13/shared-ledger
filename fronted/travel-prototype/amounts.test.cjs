const {test}=require('node:test');const assert=require('node:assert/strict');const {cents,allocate}=require('./amounts.js');
test('money is parsed without floating point rounding',()=>{assert.equal(cents('100.01'),10001);assert.throws(()=>cents('1.001'));});
test('equal split conserves cents with stable remainder',()=>assert.deepEqual(allocate(10001,'AVERAGE',[1,1,1]),[3334,3334,3333]));
test('weighted split conserves large amounts',()=>{const result=allocate(100000000,'RATIO',['999999.99','111111.11','1']);assert.equal(result.reduce((a,b)=>a+b,0),100000000);assert.ok(result.every(x=>x>0));});
test('reject impossible and mismatched splits',()=>{assert.throws(()=>allocate(1,'AVERAGE',[1,1]));assert.throws(()=>allocate(100,'FIXED_AMOUNT',['1','1']));});
