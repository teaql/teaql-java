#!/usr/bin/env python3
import csv,statistics
from pathlib import Path
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
b=Path(__file__).resolve().parent
original=next(p for p in sorted(b.glob('results-*'),reverse=True) if (p/'startup-summary.json').exists())
current=next(p for p in sorted(b.glob('current-results-*'),reverse=True) if (p/'startup-summary.json').exists())
oldrows=list(csv.DictReader((b.parent/'matched-six/results-20261009-220739/startup.csv').open()));rows=list(csv.DictReader((original/'startup.csv').open()));newrows=list(csv.DictReader((current/'startup.csv').open()))
plt.rcParams.update({'font.family':'DejaVu Sans','font.size':10,'svg.fonttype':'none','axes.spines.top':False,'axes.spines.right':False})
for name,title,groups,modes in [
 ('06-jdk-runtime','Same deployed Java 21 bundles: runtime changes',[(v,l) for v,l in [('legacy-spring','Legacy Spring'),('modern-spring','Modern Spring'),('modern-classpath','Standalone'),('modern-jpms','JPMS'),('modern-quarkus','Quarkus'),('modern-micronaut','Micronaut')]],[(oldrows,'Java 21 / Corretto','', '#94a3b8'),(rows,'Java 25 / Oracle','-jdk25','#0f766e'),(rows,'Java 27 / Oracle','-jdk27','#2563eb')]),
 ('07-current-frameworks','Current frameworks: identical Java 25 wrappers on two JVMs',[(f'current-{v}',l) for v,l in [('spring','Spring Boot 4.1.1'),('quarkus','Quarkus 3.40.1'),('micronaut','Micronaut 5.2.15')]],[(newrows,'Java 25 / Oracle','-jdk25','#0f766e'),(newrows,'Java 27 / Oracle','-jdk27','#2563eb')])]:
 fig,axes=plt.subplots(1,2,figsize=(12,4.5),layout='constrained');width=.7/len(modes)
 for ax,key,label in [(axes[0],'business_ready_s','Seconds to first business read'),(axes[1],'business_ready_rss_mib','RSS after read (MiB)')]:
  for i,(v,l) in enumerate(groups):
   label_base=max(sorted(float(x[key]) for x in rr if x['variant']==v+suffix)[26] for rr,mode,suffix,color in modes)
   for j,(rr,mode,suffix,color) in enumerate(modes):
    values=sorted(float(x[key]) for x in rr if x['variant']==v+suffix);m=statistics.median(values);x=i+(j-(len(modes)-1)/2)*width
    ax.bar(x,m,width=width*.9,color=color,label=mode if i==0 else None);ax.errorbar(x,m,yerr=[[m-values[2]],[values[26]-m]],fmt='none',ecolor='#334155',capsize=2,lw=1)
    ax.annotate(f'{m:.2f}' if key.endswith('_s') else f'{m:.0f}',(x,label_base if len(modes)==3 else values[26]),xytext=(0,6+12*j if len(modes)==3 else 6),textcoords='offset points',ha='center',fontsize=8)
  ax.set_xticks(range(len(groups)),[l.replace(' ','\n') for _,l in groups]);ax.set_ylabel(label);ax.set_ylim(0,ax.get_ylim()[1]*1.2);ax.grid(axis='y',color='#e2e8f0');ax.set_axisbelow(True);ax.legend(frameon=False,fontsize=8)
 fig.suptitle(title,fontsize=14,fontweight='bold');fig.supxlabel('30 fresh JVMs per variant · median and p10–p90 · -Xms128m/-Xmx512m · OS caches not cleared',fontsize=9)
 out=b.parents[2]/'figures'/name;fig.savefig(out.with_suffix('.png'),dpi=220);fig.savefig(out.with_suffix('.svg'));print(out)

native_dirs=[p for p in sorted((b/'native25').glob('results-*')) if (p/'load-summary.json').exists()]
if native_dirs:
 native_rows=list(csv.DictReader((native_dirs[-1]/'startup.csv').open()))
 comparisons=[('legacy','Legacy Spring',rows,'legacy-spring-jdk25'),('spring','Spring Boot 4.1.1',newrows,'current-spring-jdk25'),('quarkus','Quarkus 3.40.1',newrows,'current-quarkus-jdk25'),('micronaut','Micronaut 5.2.15',newrows,'current-micronaut-jdk25'),('standalone','Standalone',rows,'modern-classpath-jdk25')]
 fig,axes=plt.subplots(1,2,figsize=(12,4.5),layout='constrained')
 for ax,key,label in [(axes[0],'business_ready_s','Seconds to first business read'),(axes[1],'business_ready_rss_mib','RSS after read (MiB)')]:
  for i,(v,name,jvm_rows,jvm_variant) in enumerate(comparisons):
   for j,(rr,variant,color,mode) in enumerate([(jvm_rows,jvm_variant,'#94a3b8','Oracle JVM 25'),(native_rows,v,'#0f766e','GraalVM 25 native')]):
    values=sorted(float(x[key]) for x in rr if x['variant']==variant);m=statistics.median(values);x=i+(j-.5)*.32
    ax.bar(x,m,width=.28,color=color,label=mode if i==0 else None)
    ax.errorbar(x,m,yerr=[[m-values[2]],[values[26]-m]],fmt='none',ecolor='#334155',capsize=2,lw=1)
    ax.annotate(f'{m:.3f}' if key.endswith('_s') else f'{m:.0f}',(x,values[26]),xytext=(0,6),textcoords='offset points',ha='center',fontsize=8)
  ax.set_xticks(range(len(comparisons)),[x[1].replace(' ','\n') for x in comparisons]);ax.set_ylabel(label);ax.set_ylim(0,ax.get_ylim()[1]*1.2);ax.grid(axis='y',color='#e2e8f0');ax.set_axisbelow(True);ax.legend(frameon=False,fontsize=8)
 fig.suptitle('Java 25 deployment stacks: JVM and Linux native',fontsize=14,fontweight='bold')
 fig.supxlabel('30 fresh processes · median and p10–p90 · same heap flags · JVM G1 / native Serial GC · OS caches not cleared',fontsize=9)
 out=b.parents[2]/'figures'/'08-native25';fig.savefig(out.with_suffix('.png'),dpi=220);fig.savefig(out.with_suffix('.svg'));print(out)
