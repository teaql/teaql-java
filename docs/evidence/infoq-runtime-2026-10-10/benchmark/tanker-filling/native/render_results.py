#!/usr/bin/env python3
import argparse,csv,statistics
from pathlib import Path
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
p=argparse.ArgumentParser();p.add_argument('native',type=Path);p.add_argument('--control',type=Path,required=True);a=p.parse_args();base=Path(__file__).resolve().parent
jvm=list(csv.DictReader((base.parent/'matched-six/results-20261009-220739/startup.csv').open()));native=list(csv.DictReader((a.native/'startup.csv').open()));control=list(csv.DictReader((a.control/'startup.csv').open()))
variants=['legacy','spring','standalone','quarkus','micronaut'];old={'legacy':'legacy-spring','spring':'modern-spring','standalone':'modern-classpath','quarkus':'modern-quarkus','micronaut':'micronaut'}
variants=[v for v in variants if any(r['variant']==v for r in native)];labels={'legacy':'Legacy\nSpring','spring':'Modern\nSpring','standalone':'Modern\nstandalone','quarkus':'Modern\nQuarkus','micronaut':'Modern\nMicronaut*'}
plt.rcParams.update({'font.family':'DejaVu Sans','font.size':10,'svg.fonttype':'none','axes.spines.top':False,'axes.spines.right':False})
fig,axes=plt.subplots(1,2,figsize=(12,4.5),layout='constrained')
for ax,key,title,unit in [(axes[0],'business_ready_s','First successful business read','Seconds from process launch'),(axes[1],'business_ready_rss_mib','Memory after first business read','Process RSS (MiB)')]:
 for i,v in enumerate(variants):
  for offset,rows,label,color in [(-.18,control if v=='micronaut' else jvm,'JVM','#94a3b8'),(.18,native,'Native','#0f766e')]:
   group=v if label=='Native' else old[v];values=sorted(float(r[key]) for r in rows if r['variant']==group);m=statistics.median(values)
   ax.bar(i+offset,m,width=.32,color=color,label=label if i==0 else None)
   ax.errorbar(i+offset,m,yerr=[[m-values[2]],[values[26]-m]],fmt='none',ecolor='#334155',capsize=2,lw=1)
   ax.annotate(f'{m:.3f}' if key.endswith('_s') else f'{m:.0f}',(i+offset,values[26]),xytext=(0,7),textcoords='offset points',ha='center',fontsize=8)
 ax.set_xticks(range(len(variants)),[labels[v] for v in variants]);ax.set_title(title,fontweight='bold');ax.set_ylabel(unit);ax.set_ylim(0,ax.get_ylim()[1]*1.18);ax.grid(axis='y',color='#e2e8f0');ax.set_axisbelow(True);ax.legend(frameon=False)
fig.suptitle('Same business slice: JVM and Linux native executables',fontsize=15,fontweight='bold')
fig.supxlabel('30 fresh processes per mode · median with p10–p90 · OS cache not cleared\n* Micronaut pairs use Jackson 2.16.1; Native Serial GC and JVM default GC differ',fontsize=9,color='#475569')
out=base.parents[2]/'figures/05-native-startup';fig.savefig(out.with_suffix('.png'),dpi=220);fig.savefig(out.with_suffix('.svg'));print(out)
