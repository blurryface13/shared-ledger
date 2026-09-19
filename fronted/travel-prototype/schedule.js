/* Builds a preview only. Saving still goes through aggregate version validation. */
(function(root){
  function minute(t){if(!/^\d{2}:\d{2}$/.test(t))throw Error('时间格式无效');return Number(t.slice(0,2))*60+Number(t.slice(3));}
  function reorder(items,source,target){
    const original=[...items].sort((a,b)=>minute(a.time)-minute(b.time));
    const from=original.findIndex(x=>x.id===source),to=original.findIndex(x=>x.id===target);
    if(from<0||to<0||original.some(x=>x.day!==original[0].day))throw Error('只能调整当天的安排');
    if(from===to)return original.map(x=>({...x}));
    const gaps=original.slice(1).map((x,i)=>Math.max(0,minute(x.time)-minute(original[i].time)-original[i].duration));
    const next=original.map(x=>({...x}));next.splice(to,0,next.splice(from,1)[0]);let cursor=minute(original[0].time);
    next.forEach((x,i)=>{x.time=String(Math.floor(cursor/60)).padStart(2,'0')+':'+String(cursor%60).padStart(2,'0');cursor+=x.duration;if(cursor>1440)throw Error('调整后超出当天，请先缩短停留时间');cursor+=gaps[i]||0;});
    for(const old of original)if(old.locked||old.done){const index=next.findIndex(x=>x.id===old.id);if(next[index].time!==old.time||index!==original.indexOf(old))throw Error('请先解锁或取消打卡，再调整相关安排');}
    return next;
  }
  if(typeof module!=='undefined')module.exports={reorder};else root.scheduleTools={reorder};
})(typeof window!=='undefined'?window:null);
