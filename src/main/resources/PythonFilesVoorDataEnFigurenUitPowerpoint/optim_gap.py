#!/usr/bin/env python3
import sys
import math
import statistics
from pathlib import Path
from collections import defaultdict
import csv

def lees_instantie(pad):
    with open(pad, 'r') as f:
        eerste_regel = next(f).split()
        aantal_items = int(eerste_regel[0])
        aantal_types = int(eerste_regel[1])

        totaal_oppervlakte = 0.0
        
        for regel in f:
            delen = regel.split()
            
            aantal = int(delen[0])
            lengte = float(delen[1])
            p1 = float(delen[2])
            p2 = float(delen[3])
            
            totaal_oppervlakte += aantal * (2.0 * lengte - (p1 + p2))

    return aantal_items, aantal_types, totaal_oppervlakte

def lees_oplossing(pad):
    with open(pad, 'r') as f:
        regels = f.readlines()
    huidige_onderGrens = 0.0
    huidige_bovenGrens = 0.0
    laatste_onderGrens = 0.0
    laatste_bovenGrens = 0.0
    huidige_stap = None
    for line in regels:
        line = line.strip()
        if line.startswith("STEP="):
            if huidige_stap is not None:
                laatste_onderGrens = huidige_onderGrens
                laatste_bovenGrens = huidige_bovenGrens
            huidige_stap = line[5:].strip()
        elif line.startswith("LB="):
            huidige_onderGrens = float(line[3:])
        elif line.startswith("UB="):
            huidige_bovenGrens = float(line[3:])
            
    if huidige_stap is not None:
        laatste_onderGrens = huidige_onderGrens
        laatste_bovenGrens = huidige_bovenGrens
    return laatste_bovenGrens, laatste_onderGrens

def bepaal_instantie_type(instantie_naam):
    basis = Path(instantie_naam).name
    if basis[0].lower() == 'a':
        return 'a'
    elif basis[0].lower() == 'r':
        return 'r'
    return 'onbekend'

def main():
    if len(sys.argv) < 3:
        print("Gebruik: python samenvatting.py <map_oplossingen> <map_originele_instanties>")
        sys.exit(1)

    oplossingen_map = Path(sys.argv[1])
    instanties_map = Path(sys.argv[2])

    oplossing_bestanden = sorted(oplossingen_map.glob('*.txt'))
    instantie_bestanden = sorted(instanties_map.glob('*.txt'))

    n = min(len(oplossing_bestanden), len(instantie_bestanden))

    records = []
    overgeslagen = 0
    for idx in range(n-1):
        oplossing_bestand = oplossing_bestanden[idx]
        instantie_bestand = instantie_bestanden[idx]

        ub, lb = lees_oplossing(oplossing_bestand)
        items, types_aantal, oppervlakte = lees_instantie(instantie_bestand)

        if items is None or oppervlakte is None or oppervlakte == 0.0:
            print(f"Waarschuwing: kan gegevens niet lezen voor {instantie_bestand.name}, overgeslagen.")
            continue

        instantie_type = bepaal_instantie_type(instantie_bestand.name)
        if instantie_type == 'a' and items not in (100, 200, 300):
            overgeslagen += 1
            continue

        tmin = math.ceil(oppervlakte / 8400.0)
        records.append({
            'bestand': oplossing_bestand.name,
            'instantie_type': instantie_type,
            'aantal_items': items,
            'aantal_types': types_aantal,
            'tmin': tmin,
            'ub': ub,
            'lb': lb,
            'ceil_lb': math.ceil(lb)
        })

    groepen = defaultdict(list)
    for r in records:
        sleutel = (r['instantie_type'], r['aantal_items'])
        groepen[sleutel].append(r)

    samenvatting = []
    for (type_, aantal_items), groep in sorted(groepen.items()):
        geldig = [g for g in groep if g['ub'] > 0]
        if not geldig:
            continue
        aantal_instanties = len(groep)
        types_gemiddelde = statistics.mean(g['aantal_types'] for g in groep)
        tmin_waarden = [g['tmin'] for g in groep]
        tmin_gemiddelde = statistics.mean(tmin_waarden)
        tmin_std = statistics.stdev(tmin_waarden)
        ub_waarden = [g['ub'] for g in geldig]
        ub_gemiddelde = statistics.mean(ub_waarden)
        ub_std = statistics.stdev(ub_waarden)
        lb_waarden = [g['lb'] for g in geldig]
        lb_gemiddelde = statistics.mean(lb_waarden)
        lb_std = statistics.stdev(lb_waarden)
        tmin_hits = sum(1 for g in geldig if g['ub'] == g['tmin'])
        tmin_pct = (tmin_hits / len(geldig)) * 100
        optimaal_aantal = sum(1 for g in geldig if g['ub'] == g['ceil_lb'])
        opt_pct = (optimaal_aantal / len(geldig)) * 100

        samenvatting.append({
            'instantie_type': type_,
            'aantal_items': aantal_items,
            'aantal_instanties': aantal_instanties,
            'types_gemiddelde': types_gemiddelde,
            'tmin_gemiddelde': tmin_gemiddelde,
            'tmin_std': tmin_std,
            'ub_gemiddelde': ub_gemiddelde,
            'ub_std': ub_std,
            'lb_gemiddelde': lb_gemiddelde,
            'lb_std': lb_std,
            'tmin_pct': tmin_pct,
            'opt_pct': opt_pct
        })

    print(f"{'Type':<4} {'Items':<6} {'#Inst':<6} {'#Types':>7} {'TMin (gem ± std)':>22} "
          f"{'UB (gem ± std)':>20} {'LB (gem ± std)':>20} {'TMin%':>7} {'Opt%':>7}")
    print("-" * 115)
    for r in sorted(samenvatting, key=lambda x: (x['instantie_type'], x['aantal_items'])):
        tmin_tekst = f"{r['tmin_gemiddelde']:.2f} ± {r['tmin_std']:.2f}"
        ub_tekst = f"{r['ub_gemiddelde']:.2f} ± {r['ub_std']:.2f}"
        lb_tekst = f"{r['lb_gemiddelde']:.2f} ± {r['lb_std']:.2f}"
        print(f"{r['instantie_type']:<4} {r['aantal_items']:<6} {r['aantal_instanties']:<6} "
              f"{r['types_gemiddelde']:7.1f} {tmin_tekst:>22} {ub_tekst:>20} {lb_tekst:>20} {r['tmin_pct']:6.1f}% {r['opt_pct']:6.1f}%")

    csv_bestandsnaam = "samenvatting_tabel.csv"
    with open(csv_bestandsnaam, 'w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=['instantie_type','aantal_items','aantal_instanties',
                                              'types_gemiddelde','tmin_gemiddelde','tmin_std',
                                              'ub_gemiddelde','ub_std','lb_gemiddelde','lb_std',
                                              'tmin_pct','opt_pct'])
        writer.writeheader()
        writer.writerows(samenvatting)

if __name__ == '__main__':
    main()