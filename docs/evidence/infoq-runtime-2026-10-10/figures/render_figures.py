"""Render original article diagrams as SVG and 2x PNG using Pillow.

Run with Python 3 and Pillow. Coordinates, text, and semantics are kept here
so the submission images can be revised without a proprietary editor.
"""
from pathlib import Path
from html import escape
import math
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parent
INK, LINE = '#183247', '#536b7c'
BLUE, GREEN, AMBER, GRAY = '#e8f2fb', '#eaf5ef', '#fff3dc', '#f0f3f5'
FONT = '/System/Library/Fonts/Supplemental/Arial.ttf'
BOLD = '/System/Library/Fonts/Supplemental/Arial Bold.ttf'


class Figure:
    def __init__(self, title, subtitle, height):
        self.w, self.h = 1200, height
        self.svg = [f'<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="{height}" viewBox="0 0 1200 {height}">', '<rect width="100%" height="100%" fill="white"/>']
        self.im = Image.new('RGB', (2400, height * 2), 'white')
        self.d = ImageDraw.Draw(self.im)
        self.text(40, 44, title, 30, True)
        self.text(40, 80, subtitle, 19)

    def text(self, x, y, s, size=21, bold=False, center=False):
        font = ImageFont.truetype(BOLD if bold else FONT, size * 2)
        anchor = 'middle' if center else 'start'
        self.svg.append(f'<text x="{x}" y="{y}" fill="{INK}" font-family="Arial, sans-serif" font-size="{size}" font-weight="{700 if bold else 400}" text-anchor="{anchor}">{escape(s)}</text>')
        self.d.text((x * 2, y * 2), s, font=font, fill=INK, anchor='ms' if center else 'ls')

    def box(self, x, y, w, h, title, lines=(), fill=BLUE):
        self.svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="10" fill="{fill}" stroke="{LINE}" stroke-width="1.5"/>')
        self.d.rounded_rectangle((x*2,y*2,(x+w)*2,(y+h)*2), radius=20, fill=fill, outline=LINE, width=3)
        self.text(x+w/2, y+32, title, 22, True, True)
        for i, line in enumerate(lines):
            self.text(x+w/2, y+61+i*25, line, 19, center=True)

    def arrow(self, points, label=None, lx=None, ly=None):
        coords = ' '.join(f'{x},{y}' for x,y in points)
        self.svg.append(f'<polyline points="{coords}" fill="none" stroke="{LINE}" stroke-width="2.5"/>')
        self.d.line([(x*2,y*2) for x,y in points], fill=LINE, width=5)
        x,y=points[-1]; a,b=points[-2]; angle=math.atan2(y-b,x-a)
        tri=[(x,y),(x-12*math.cos(angle-.45),y-12*math.sin(angle-.45)),(x-12*math.cos(angle+.45),y-12*math.sin(angle+.45))]
        self.svg.append('<polygon points="'+' '.join(f'{a},{b}' for a,b in tri)+f'" fill="{LINE}"/>')
        self.d.polygon([(a*2,b*2) for a,b in tri],fill=LINE)
        if label: self.text(lx,ly,label,18,center=True)

    def save(self, name):
        (OUT / f'{name}.svg').write_text('\n'.join(self.svg+['</svg>'])+'\n')
        self.im.save(OUT / f'{name}.png')


f = Figure('1. Extract the platform boundary', 'Conceptual dependency view: arrows point from consumers to the contracts or APIs they use.', 800)
f.text(215, 130, 'Before: Spring-oriented persistence', 22, True, True)
f.text(805, 130, 'After: explicit contracts and adapters', 22, True, True)
f.box(50,165,330,95,'Business operation',['Domain query and entity API'])
f.box(50,315,330,120,'SQLRepository',['SQL translation + execution','Transaction integration'],AMBER)
f.box(50,490,330,120,'Spring infrastructure',['NamedParameterJdbcTemplate','TransactionTemplate'],GRAY)
f.arrow([(215,260),(215,315)])
f.arrow([(215,435),(215,490)])
f.arrow([(410,375),(500,375)],'refactor',455,350)
f.box(560,165,510,95,'Business operation',['Generated domain API + execution context'])
f.box(560,315,510,120,'Shared TeaQL contracts and runtime',['Metadata, policy, data-service contracts','Portable SQL translation'])
f.arrow([(815,260),(815,315)])
f.box(545,490,255,120,'JDBC provider',['Connections, rows,','transactions'],GREEN)
f.box(830,490,315,120,'Android SQLite adapter',['Native database, cursors,','statements, transactions'],GREEN)
f.arrow([(672,490),(672,435)])
f.arrow([(987,490),(987,435)])
f.box(545,660,255,85,'JDBC APIs',['DataSource / driver'],GRAY)
f.box(830,660,315,85,'Android APIs',['SQLiteDatabase / Cursor'],GRAY)
f.arrow([(672,610),(672,660)])
f.arrow([(987,610),(987,660)])
f.text(40,780,'Responsibility view; selected components shown, not the complete JPMS dependency graph.',18)
f.save('01-platform-boundary')

f = Figure('2. Keep expressions stable; supply local execution resources', 'Conceptual operation flow: each application assembles its own context and storage provider.', 1000)
f.box(300,115,600,95,'Q: generated query expression',['Describe data, relations, and operation intent'])
f.box(60,265,510,115,'Server application',['Spring Boot / Quarkus / Micronaut','Application-configured UserContext'],GREEN)
f.box(630,265,510,115,'Android application',['Gradle + desugaring + D8 -> DEX / ART','Application-configured UserContext'],GREEN)
f.arrow([(450,210),(450,235),(315,235),(315,265)])
f.arrow([(750,210),(750,235),(885,235),(885,265)])
f.box(60,435,510,95,'TeaQL runtime + portable SQL',['Policy, query translation, data-service execution'])
f.box(630,435,510,95,'TeaQL runtime + portable SQL',['Policy, query translation, data-service execution'])
f.arrow([(315,380),(315,435)])
f.arrow([(885,380),(885,435)])
f.box(60,580,510,95,'JDBC execution',['DataSource -> Connection -> ResultSet'],GRAY)
f.box(630,580,510,95,'Native SQLite execution',['SQLiteDatabase -> Cursor / SQLiteStatement'],GRAY)
f.arrow([(315,530),(315,580)])
f.arrow([(885,530),(885,580)])
f.box(300,745,600,100,'Materialized domain values',['Hydrated entities + loaded-property state'])
f.arrow([(315,675),(315,710),(450,710),(450,745)])
f.arrow([(885,675),(885,710),(750,710),(750,745)])
f.box(300,890,600,75,'E: typed value traversal',[])
f.arrow([(600,845),(600,890)])
f.text(40,990,'JPMS enforces boundaries on the JVM module path. Android does not execute JPMS. E does not fetch unloaded relations.',17)
f.save('02-expression-execution')

f = Figure('3. Explicit mapping and separate output boundaries', 'Arrows show information flow or configuration; output policies apply at their own boundaries.', 1020)
f.box(40,115,345,120,'EntityDescriptor',['Registered constructor supplier','Property and relation metadata'])
f.box(460,115,700,120,'Selected row-mapping path',['Projection -> ColumnBindings -> CompiledRowMapper','DataRow supplies typed column values'])
f.arrow([(385,175),(460,175)])
f.box(300,295,600,115,'Hydrated domain entity',['createEntity() + generated access / __internalHydrate','Loaded state preserved; hydration is not a user mutation'])
f.arrow([(810,235),(810,295)])
f.arrow([(212,235),(212,352),(300,352)])
f.box(40,510,345,145,'BaseEntity JSON codec',['Explicit base identity fields','and additional values','Raw id / version when present'],GRAY)
f.box(425,510,345,145,'Audit construction',['Descriptor mask fields','Credential protection','Value limits and trace scrubbing'],GREEN)
f.box(810,510,350,145,'Round-trip reference codec',['Entity identity + trusted actor','Document / aggregate scope','Provider authorization'],AMBER)
f.arrow([(450,410),(450,460),(212,460),(212,510)])
f.arrow([(600,410),(600,510)])
f.arrow([(750,410),(750,460),(985,460),(985,510)])
f.box(40,720,345,115,'Low-level JSON',['Additional object values may','need Jackson native metadata'],GRAY)
f.box(425,720,345,115,'Safe audit fields',['Masking and truncation','apply to audit output'],GREEN)
f.box(810,720,350,115,'Protected token',['AES-GCM + fresh nonce','Scope, expiry, authorization'],AMBER)
f.arrow([(212,655),(212,720)])
f.arrow([(597,655),(597,720)])
f.arrow([(985,655),(985,720)])
f.box(40,890,1120,85,'Scope of the implemented protection',['Document ID is bound inside the reference token; a full protected-document protocol remains separate.'],GRAY)
f.text(40,1005,'These are separate paths. The base JSON codec does not automatically apply audit masking or protected-reference encoding.',18)
f.save('03-mapping-output-boundaries')
