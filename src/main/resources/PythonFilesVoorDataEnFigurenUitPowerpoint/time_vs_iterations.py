#!/usr/bin/env python3
import sys
from pathlib import Path
import matplotlib.pyplot as plt

def lees_totale_tijd(pad):
    totale_tijd = 0.0
    with open(pad, 'r') as f:
        for line in f:
            if line.startswith("ITERATION_TIME="):
                totale_tijd += float(line[15:].strip())
    return totale_tijd

def lees_laatste_iteratie(pad):
    laatste = 0
    with open(pad, 'r') as f:
        for line in f:
            if line.startswith("ITERATION="):
                laatste = int(line[10:].strip())
    return laatste

def main():
    if len(sys.argv) < 2:
        print("Gebruik: python tijd_vs_iteraties_apart.py <map_met_oplossingen>")
        sys.exit(1)

    opl_map = Path(sys.argv[1])
    bestanden = sorted(opl_map.glob('*.txt'))

    punten_a = []
    punten_r = []

    for bestand in bestanden[:-1]:
        naam = bestand.name.split('_')[0]
        type_code = naam[0].lower()
        aantal_honderdtallen = int(naam[1])
        
        if type_code == 'a' and aantal_honderdtallen not in (1, 2, 3):
            continue

        tijd = lees_totale_tijd(bestand)
        iteraties = lees_laatste_iteratie(bestand)

        if type_code == 'a':
            punten_a.append((tijd, iteraties))
        else:
            punten_r.append((tijd, iteraties))

    # grafiek voor a
    #splitten van punten_a in t_a en i_a
    t_a, i_a = zip(*punten_a)
    plt.figure(figsize=(10, 6))
    plt.scatter(t_a, i_a, c='steelblue', label='a (unieke items)', edgecolors='k')
    plt.xlabel('Totale rekentijd (ms)')
    plt.ylabel('Aantal iteraties')
    plt.title('Iteraties vs. rekentijd (a‑instanties)')
    plt.legend()
    plt.grid(True, linestyle='--', alpha=0.5)
    plt.tight_layout()
    plt.savefig('plot_tijd_vs_iteraties_a.png', dpi=150)
    plt.show()

    # grafiek voor r
    t_r, i_r = zip(*punten_r)
    plt.figure(figsize=(10, 6))
    plt.scatter(t_r, i_r, c='darkorange', label='r (realistisch)', edgecolors='k')
    plt.xlabel('Totale rekentijd (ms)')
    plt.ylabel('Aantal iteraties')
    plt.title('Iteraties vs. rekentijd (r‑instanties)')
    plt.legend()
    plt.grid(True, linestyle='--', alpha=0.5)
    plt.tight_layout()
    plt.savefig('plot_tijd_vs_iteraties_r.png', dpi=150)
    plt.show()

if __name__ == '__main__':
    main()