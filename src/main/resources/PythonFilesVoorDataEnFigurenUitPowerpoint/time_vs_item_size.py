#!/usr/bin/env python3
import sys
from pathlib import Path
import matplotlib.pyplot as plt

def lees_totale_tijd(pad):
    with open(pad, 'r') as bestand:
        regels = bestand.readlines()
    totale_tijd = 0.0
    for regel in regels:
        if regel.startswith("ITERATION_TIME="):
            totale_tijd += float(regel[15:].strip())
    return totale_tijd

def main():
    if len(sys.argv) < 2:
        print("Gebruik: python time_vs_item_type_size.py <map_met_oplossingen>")
        sys.exit(1)

    oplossingen_map = Path(sys.argv[1])
    oplossing_bestanden = sorted(oplossingen_map.glob('*.txt'))

    punten_a = []
    punten_r = []

    for bestand in oplossing_bestanden[:-1]:
        naam = bestand.name.split('_')[0]
        type_code = naam[0].lower()
        aantal_honderdtallen = int(naam[1])

        if type_code == 'a' and aantal_honderdtallen not in (1, 2, 3):
            continue

        tijd = lees_totale_tijd(bestand)
        aantal_items = aantal_honderdtallen * 100

        if type_code == 'a':
            punten_a.append((aantal_items, tijd))
        else:
            punten_r.append((aantal_items, tijd))

    plt.figure(figsize=(10, 6))
    if punten_a:
        #splitten van punten in x_a en y_a
        x_a, y_a = zip(*punten_a)
        plt.scatter(x_a, y_a, c='steelblue', label='a (unieke items)', edgecolors='k')
    if punten_r:
        x_r, y_r = zip(*punten_r)
        plt.scatter(x_r, y_r, c='darkorange', label='r (realistisch)', edgecolors='k')

    plt.xlabel('Aantal items')
    plt.ylabel('Totale rekentijd (ms)')
    plt.title('Rekentijd vs Aantal items')
    plt.xticks([100, 200, 300, 400, 500])
    plt.legend()
    plt.grid(True, linestyle='--', alpha=0.5)
    plt.tight_layout()
    plt.savefig('plot_tijd_vs_items.png', dpi=150)
    plt.show()
    
if __name__ == '__main__':
    main()