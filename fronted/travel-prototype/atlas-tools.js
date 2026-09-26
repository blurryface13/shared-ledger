/* Coordinates cross the GCJ-02 / WGS84 boundary only at the map edge. Stored POIs stay GCJ-02. */
(function(root,factory){const api=factory();if(typeof module==='object')module.exports=api;else root.atlasTools=api;})(globalThis,()=>{
  const valid=p=>p&&Number.isFinite(p.longitude)&&Number.isFinite(p.latitude)&&Math.abs(p.longitude)<=180&&Math.abs(p.latitude)<=90;
  function offset(lon,lat){
    const x=lon-105,y=lat-35,pi=Math.PI;
    let a=-100+2*x+3*y+.2*y*y+.1*x*y+.2*Math.sqrt(Math.abs(x));
    let b=300+x+2*y+.1*x*x+.1*x*y+.1*Math.sqrt(Math.abs(x));
    const wave=(20*Math.sin(6*x*pi)+20*Math.sin(2*x*pi))*2/3;
    a+=wave+(20*Math.sin(y*pi)+40*Math.sin(y*pi/3))*2/3+(160*Math.sin(y*pi/12)+320*Math.sin(y*pi/30))*2/3;
    b+=wave+(20*Math.sin(x*pi)+40*Math.sin(x*pi/3))*2/3+(150*Math.sin(x*pi/12)+300*Math.sin(x*pi/30))*2/3;
    const rad=lat*pi/180,m=1-.00669342162296594323*Math.sin(rad)**2;
    return [b*180/(6378245/Math.sqrt(m)*Math.cos(rad)*pi),a*180/((6378245*(1-.00669342162296594323))/(m*Math.sqrt(m))*pi)];
  }
  function toWgs(lon,lat,system){
    if(!valid({longitude:lon,latitude:lat}))return null;
    if(system==='WGS84')return [lat,lon];
    if(lon<72.004||lon>137.8347||lat<.8293||lat>55.8271)return [lat,lon];
    let x=lon,y=lat;for(let i=0;i<5;i++){const [dx,dy]=offset(x,y);x=lon-dx;y=lat-dy;}return [y,x];
  }
  function distance(a,b){if(!valid(a)||!valid(b))return null;const p=toWgs(a.longitude,a.latitude,a.coordinateSystem),q=toWgs(b.longitude,b.latitude,b.coordinateSystem),r=Math.PI/180;const h=Math.sin((q[0]-p[0])*r/2)**2+Math.cos(p[0]*r)*Math.cos(q[0]*r)*Math.sin((q[1]-p[1])*r/2)**2;return 6371000*2*Math.asin(Math.sqrt(Math.min(1,h)));}
  const meters=n=>n==null?'':n<1000?`${Math.round(n/10)*10} m`:`${(n/1000).toFixed(1)} km`;
  const ordered=(items,day)=>items.filter(a=>day===-1||a.day===day).slice().sort((a,b)=>a.day-b.day||a.time.localeCompare(b.time));
  const dateAt=(start,day)=>new Date(Date.parse(start+'T00:00:00Z')+day*86400000).toISOString().slice(0,10);
  function forecast(data,date){const i=data?.daily?.time?.indexOf(date)??-1;if(i<0)return null;const d=data.daily,lo=d.temperature_2m_min?.[i],hi=d.temperature_2m_max?.[i];return Number.isFinite(lo)&&Number.isFinite(hi)?{low:Math.round(lo),high:Math.round(hi),rain:d.precipitation_probability_max?.[i],code:d.weather_code?.[i]}:null;}
  return {valid,toWgs,distance,meters,ordered,dateAt,forecast};
});
