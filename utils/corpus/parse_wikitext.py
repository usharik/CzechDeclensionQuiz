import json, re, sys
def cs_section(t):
    m=re.search(r'==\s*čeština\s*==(.*?)(?=\n==[^=]|\Z)', t, re.S)
    return m.group(1) if m else ''
def clean(v):
    v=re.sub(r'<ref[^>]*/>','',v); v=re.sub(r'<ref[^>]*>.*?</ref>','',v,flags=re.S)
    v=re.sub(r'<[^>]*>',' ',v)  # <br />, <small> and friends
    v=re.sub(r"''[^']*''",'',v)
    parts=[]
    for p in v.split('/'):
        if '{{Příznak' in p or '{{příznak' in p: continue
        p=re.sub(r'\[\[([^\]|]*\|)?([^\]]*)\]\]', r'\2', p)
        p=re.sub(r'\{\{[^}]*\}\}','',p).strip(' ,;')
        if p and p not in ('zobrazit','skrýt','-','—'): parts.append(p)
    return ', '.join(dict.fromkeys(parts))
def template_params(sec, name):
    i=sec.find('{{'+name)
    if i<0: return None
    depth=0; j=i
    while j<len(sec):
        if sec.startswith('{{',j): depth+=1; j+=2; continue
        if sec.startswith('}}',j):
            depth-=1; j+=2
            if depth==0: break
            continue
        j+=1
    body=sec[i+2:j-2]
    params={}
    # split on top-level '|'
    depth=0; cur=''; items=[]
    for ch in body:
        if ch=='{' : depth+=1
        if ch=='}' : depth-=1
        if ch=='[' : depth+=1
        if ch==']' : depth-=1
        if ch=='|' and depth==0: items.append(cur); cur=''
        else: cur+=ch
    items.append(cur)
    for it in items[1:]:
        if '=' in it:
            k,v=it.split('=',1); params[k.strip()]=v.strip()
    return params
def translations(sec, lang):
    # first Překlady block(s)
    out=[]
    for m in re.finditer(r'\{\{Překlady(.*?)\n\}\}', sec, re.S):
        blk=m.group(1)
        mm=re.search(r'\n\s*\|\s*'+lang+r'\s*=\s*(.*)', blk)
        if mm:
            vals=re.findall(r'\{\{P\|'+lang+r'\|([^}|]*)', mm.group(1))
            out.append(', '.join(v.strip() for v in vals if v.strip()))
        if len(out)>=1: break   # primary sense only
    return '; '.join(o for o in out if o)
def en_gloss(en_text):
    m=re.search(r'==\s*Czech\s*==(.*?)(?=\n==[^=]|\Z)', en_text or '', re.S)
    if not m: return '', {}
    sec=m.group(1)
    head={}
    hm=re.search(r'\{\{cs-(verb|adj)\|?([^}]*)\}\}', sec)
    if hm:
        for it in hm.group(2).split('|'):
            if '=' in it: k,v=it.split('=',1); head[k.strip()]=v.strip()
            elif it.strip(): head.setdefault('_pos', []).append(it.strip())
    glosses=[]
    for line in sec.split('\n'):
        if line.startswith('# ') and not line.startswith('#*') and not line.startswith('#:'):
            g=line[2:]
            g=re.sub(r'<!--.*?-->','',g); g=re.sub(r"''[^']*''",'',g); g=g.replace('&#32;',' ')
            g=re.sub(r'\{\{lb\|cs\|[^}]*\}\}\s*','',g)
            g=re.sub(r'\{\{(?:l|m)\|en\|([^}|]*)[^}]*\}\}', r'\1', g)
            g=re.sub(r'\{\{gloss\|([^}]*)\}\}', r'(\1)', g)
            g=re.sub(r'\{\{[^}]*\}\}','',g)
            g=re.sub(r'\[\[([^\]|]*\|)?([^\]]*)\]\]', r'\2', g)
            g=g.strip(' .;')
            if g and len(glosses)<3: glosses.append(g)
    return '; '.join(glosses), head

CASES=['nom','gen','dat','acc','voc','loc','ins']; GEN=['ma','m','f','n']
def parse_adj(word, cs, en):
    sec=cs_section(cs)
    # adjective subsection
    am=re.search(r'===\s*přídavné jméno\s*===(.*?)(?=\n===[^=]|\Z)', sec, re.S)
    if not am: return None
    asec=am.group(1)
    p=template_params(asec,'Adjektivum (cs)')
    if not p: return None
    kind=re.search(r"^\*\s*''([^']*)''", asec, re.M)
    kind=kind.group(1).strip() if kind else ''
    cases=[]
    for num in 'sp':
        g_rows=[]
        for g in GEN:
            row=[]
            for c in CASES:
                key=f'{num}{c}{g}'
                v=p.get(key)
                if v is None and g=='ma': v=p.get(f'{num}{c}m')
                if v is None and c=='voc': v=p.get(f'{num}nom{g}') or p.get(f'{num}nomm')
                if v is None and g=='m' : v=p.get(f'{num}{c}ma')
                row.append(clean(v) if v is not None else '')
            g_rows.append(row)
        cases.append(g_rows)
    st=template_params(asec,'Stupňování (cs)') or {}
    gloss, head = en_gloss(en)
    return {'word':word,'kind':kind,'cases':cases,'comparative':clean(st.get('komp','')),'superlative':clean(st.get('sup','')),
            'ru':translations(asec,'ru'),'en_cs':translations(asec,'en'),'en':gloss, 'head':head}
def parse_verb(word, cs, en):
    sec=cs_section(cs)
    vm=re.search(r'===\s*sloveso\s*===(.*?)(?=\n===[^=]|\Z)', sec, re.S)
    if not vm: return None
    vsec=vm.group(1)
    p=template_params(vsec,'Sloveso (cs)')
    if not p: return None
    am=re.search(r"^\*\s*''(nedokonavé|dokonavé|obouvidové)''", vsec, re.M)
    aspect={'nedokonavé':'impf','dokonavé':'pf','obouvidové':'bi'}.get(am.group(1) if am else '', '')
    if not am and p.get('dok','').strip()=='ano': aspect='pf'
    present=[clean(p.get(k,'')) for k in ['spre1','spre2','spre3','ppre1','ppre2','ppre3']]
    future=[clean(p.get(k,'')) for k in ['sfut1','sfut2','sfut3','pfut1','pfut2','pfut3']]
    past=[clean(p.get(k,'')) for k in ['sactm','sactf','sactn','pactm','pactf']]
    imper=[clean(p.get(k,'')) for k in ['simp2','pimp1','pimp2']]
    gloss, head = en_gloss(en)
    return {'word':word,'aspect':aspect,'present':present,'future':future,'past':past,'imperative':imper,
            'ru':translations(vsec,'ru'),'en_cs':translations(vsec,'en'),'en':gloss,'head':head}
if __name__=='__main__':
    cand=json.load(open('candidates.json'))
    csv=json.load(open('cs_verbs_wikitext.json')); env=json.load(open('en_verbs_wikitext.json'))
    csa=json.load(open('cs_adjs_wikitext.json')); ena=json.load(open('en_adjs_wikitext.json'))
    verbs=[]; adjs=[]
    for w in cand['verbs']:
        r=parse_verb(w, csv.get(w,''), env.get(w,''))
        if r: verbs.append(r)
    for w in cand['adjs']:
        r=parse_adj(w, csa.get(w,''), ena.get(w,''))
        if r: adjs.append(r)
    json.dump(verbs, open('parsed_verbs.json','w'), ensure_ascii=False, indent=0)
    json.dump(adjs, open('parsed_adjs.json','w'), ensure_ascii=False, indent=0)
    def stat(name, items, fullcheck):
        full=[i for i in items if fullcheck(i)]
        ru=[i for i in full if i['ru']]; en=[i for i in full if i['en'] or i['en_cs']]
        print(name, 'parsed',len(items),'full',len(full),'with ru',len(ru),'with en',len(en), 'with both', len([i for i in full if i['ru'] and (i['en'] or i['en_cs'])]))
        return full
    fv=stat('verbs', verbs, lambda v: all(v['present']) and all(v['past']) and all(v['imperative']))
    fa=stat('adjs', adjs, lambda a: all(all(all(c for c in row) for row in num) for num in a['cases']))
    print('verbs missing parts:', [(v['word'],[k for k in ('present','past','imperative') if not all(v[k])]) for v in verbs if v not in fv][:40])
    print('adjs incomplete:', [a['word'] for a in adjs if a not in fa][:60])
    print('verbs no ru:', [v['word'] for v in fv if not v['ru']][:80])
    print('adjs no ru:', [a['word'] for a in fa if not a['ru']][:80])
    print('aspects:', {k:sum(1 for v in fv if v['aspect']==k) for k in ('impf','pf','bi','')})
    print('kinds:', {k:sum(1 for a in fa if a['kind']==k) for k in set(a['kind'] for a in fa)})
    print(json.dumps(fv[4], ensure_ascii=False)); print(json.dumps(fa[0], ensure_ascii=False))
