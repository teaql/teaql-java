#!/usr/bin/env python3
"""Render retained raw observations with matplotlib; no inferred cold-cache claim."""
import argparse,csv,random
from pathlib import Path
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
p=argparse.ArgumentParser();p.add_argument('results',type=Path);p.add_argument('--output',type=Path,default=Path(__file__).resolve().parents[3]/'figures/04-multiframework-startup');a=p.parse_args()
rows=list(csv.DictReader((a.results/'startup.csv').open()));variants=['legacy-spring','modern-spring','modern-classpath','modern-jpms','modern-quarkus','modern-micronaut'];labels=['Legacy\nSpring','Modern\nSpring','Modern\nclasspath','Modern\nJPMS','Modern\nQuarkus','Modern\nMicronaut'];colors=['#64748b','#2563eb','#0f766e','#7c3aed','#c2410c','#0369a1']
plt.rcParams.update({'font.family':'DejaVu Sans','font.size':10,'svg.fonttype':'none','axes.spines.top':False,'axes.spines.right':False})
fig,axes=plt.subplots(1,2,figsize=(12,4.5),layout='constrained');rng=random.Random(20261009)
for ax,key,title,unit in [(axes[0],'business_ready_s','First successful business read','Seconds from process launch'),(axes[1],'business_ready_rss_mib','Memory after first business read','Process RSS (MiB)')]:
 data=[[float(r[key]) for r in rows if r['variant']==v] for v in variants]
 bp=ax.boxplot(data,tick_labels=labels,showfliers=False,patch_artist=True,widths=.45,medianprops={'color':'#0f172a','linewidth':1.8})
 for i,(values,color,box) in enumerate(zip(data,colors,bp['boxes']),1):
  box.set_facecolor(color);box.set_alpha(.2);box.set_edgecolor(color)
  ax.scatter([i+rng.uniform(-.13,.13) for _ in values],values,s=10,color=color,alpha=.65,zorder=3)
 ax.set_title(title,fontweight='bold',pad=12);ax.set_ylabel(unit);ax.set_ylim(bottom=0);ax.grid(axis='y',color='#e2e8f0');ax.set_axisbelow(True)
fig.suptitle('Same business slice, six launchers',fontsize=15,fontweight='bold')
fig.supxlabel('30 fresh JVMs per variant · Java 21 · randomized blocks · OS cache not cleared',fontsize=9,color='#475569')
a.output.parent.mkdir(parents=True,exist_ok=True)
fig.savefig(a.output.with_suffix('.png'),dpi=220);fig.savefig(a.output.with_suffix('.svg'))
print(a.output)
