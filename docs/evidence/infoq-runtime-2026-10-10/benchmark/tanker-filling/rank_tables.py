"""Annotate display values, leaving raw measurements unchanged."""
import re
from pathlib import Path
LEGEND = 'Table guide: ↓ lower is better; ↑ higher is better. ①/②/③ mark the three best distinct displayed values within each table; displayed ties share a rank. Paired p50/p95 values are ranked separately. Thread counts are descriptive. These marks do not imply statistical significance or an overall framework ranking.'

def number(s):
    s=re.sub(r'[①②③]', '', s).strip()
    if re.fullmatch(r'\d+:\d+(?:\.\d+)?',s):
        m,sec=s.split(':');return float(m)*60+float(sec)
    if re.fullmatch(r'\d+(?:\.\d+)?',s):return float(s)
    return None

def annotate(text):
    text=text.replace(LEGEND+'\n\n','')
    lines=text.splitlines();out=[];i=0;changed=False
    while i<len(lines):
        if not lines[i].startswith('|'):
            out.append(lines[i]);i+=1;continue
        block=[]
        while i<len(lines) and lines[i].startswith('|'):
            block.append(lines[i]);i+=1
        if len(block)<3:out.extend(block);continue
        rows=[[c.strip() for c in line.strip().strip('|').split('|')] for line in block]
        for col in range(1,len(rows[0])):
            h=rows[0][col].replace(' ↓','').replace(' ↑','').replace(' (descriptive)','')
            if 'Thread' in h:
                rows[0][col]=h+' (descriptive)';continue
            values=[r[col].split(' / ') for r in rows[2:]]
            if not all(all(number(v) is not None for v in vals) for vals in values):continue
            if not any(k in h.lower() for k in ['(s)','(ms)','rss','mib','cpu-s','elapsed','rps','req/s']):continue
            high=any(k in h.lower() for k in ['rps','req/s']);rows[0][col]=h+(' ↑' if high else ' ↓')
            for part in range(len(values[0])):
                ordered=sorted(set(number(v[part]) for v in values),reverse=high)
                for vals in values:
                    rank=ordered.index(number(vals[part]));v=re.sub(r'[①②③]','',vals[part]).strip()
                    vals[part]=v+(' '+ '①②③'[rank] if rank<3 else '')
            for r,vals in zip(rows[2:],values):r[col]=' / '.join(vals)
            changed=True
        out.extend('| '+' | '.join(r)+' |' for r in rows)
    result='\n'.join(out)+'\n'
    if changed:
        # Put the legend immediately before the first table.
        pos=result.index('| ');result=result[:pos]+LEGEND+'\n\n'+result[pos:]
    return result

def annotate_file(path):
    p=Path(path);p.write_text(annotate(p.read_text()))
