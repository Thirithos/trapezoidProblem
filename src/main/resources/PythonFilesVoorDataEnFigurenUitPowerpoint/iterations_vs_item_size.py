#!/usr/bin/env python3
import sys
from pathlib import Path
import matplotlib.pyplot as plt

def lees_laatste_iteratie(pad):
    with open(pad, 'r') as f:
        for line in f:
            if line.startswith("ITERATION="):
                laatste = int(line[10:].strip())
    return laatste

def main():
    if len(sys.argv) < 2:
        print("Gebruik: python iterations_vs_item_size.py <map_met_oplossingen>")
        sys.exit(1)

    oplossingen_map = Path(sys.argv[1])
    bestanden = sorted(oplossingen_map.glob('*.txt'))

    punten_a = []
    punten_r = []

    # laatste is de toyset
    for bestand in bestanden[:-1]:
        naam = bestand.name.split('_')[0]
        type_code = naam[0].lower()
        aantal_honderdtallen = int(naam[1])
        
        if type_code == 'a' and aantal_honderdtallen not in (1, 2, 3):
            continue

        iteraties = lees_laatste_iteratie(bestand)

        if type_code == 'a':
            punten_a.append((aantal_honderdtallen * 100, iteraties))
        else:
            punten_r.append((aantal_honderdtallen * 100, iteraties))

    plt.figure(figsize=(10, 6))
    if punten_a:
        # splitten van punten_a in x_a en y_a
        x_a, y_a = zip(*punten_a)
        plt.scatter(x_a, y_a, c='steelblue', label='a (unieke items)', edgecolors='k')
    if punten_r:
        # splitten van punten_r in x_r en y_r
        x_r, y_r = zip(*punten_r)
        plt.scatter(x_r, y_r, c='darkorange', label='r (realistisch)', edgecolors='k')

    plt.xlabel('Aantal items')
    plt.ylabel('Aantal iteraties')
    plt.title('Aantal iteraties vs Aantal items')
    plt.xticks([100, 200, 300, 400, 500])
    plt.legend()
    plt.grid(True, linestyle='--', alpha=0.5)
    plt.tight_layout()
    plt.savefig('plot_iteraties.png', dpi=150)
    plt.show()

if __name__ == '__main__':
    main()