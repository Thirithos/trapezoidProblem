#!/usr/bin/env python3
import sys
from pathlib import Path
import matplotlib.pyplot as plt

def lees_tijden_en_bounds(pad):
    iters, lbs, times = [], [], []

    with open(pad, 'r') as f:
        for line in f:
            if line.startswith("ITERATION="):
                iters.append(int(line.split('=')[1].strip()))
            elif line.startswith("LB="):
                lbs.append(float(line.split('=')[1].strip()))
            elif line.startswith("ITERATION_TIME="):
                times.append(float(line.split('=')[1].strip()))

    return iters, lbs, times

def main():
    if len(sys.argv) != 2:
        print("Gebruik: python verloop.py <solution_bestand>")
        sys.exit(1)

    sol_bestand = Path(sys.argv[1])
    if not sol_bestand.is_file():
        print(f"Bestand niet gevonden: {sol_bestand}")
        sys.exit(1)

    iters, lbs, times = lees_tijden_en_bounds(sol_bestand)

    fig, ax1 = plt.subplots(figsize=(12, 6))
    ax1.plot(iters, lbs, 'b-', label='Lower Bound (LB)', linewidth=2)
    ax1.set_xlabel('Iteratie')
    ax1.set_ylabel('Aantal bins (LB)')
    ax1.grid(True, linestyle='--', alpha=0.5)

    ax2 = ax1.twinx()
    ax2.plot(iters, times, 'c-', label='Iteratietijd (ms)', linewidth=1.5)
    ax2.set_ylabel('Tijd per iteratie (ms)', color='c')
    ax2.tick_params(axis='y', labelcolor='c')

    lines1, labels1 = ax1.get_legend_handles_labels()
    lines2, labels2 = ax2.get_legend_handles_labels()
    fig.legend(lines1 + lines2, labels1 + labels2, loc='upper right', bbox_to_anchor=(0.95, 0.95))

    plt.title(sol_bestand.name)
    fig.tight_layout()
    out_name = f'verloop_{sol_bestand.stem}.png'
    plt.savefig(out_name, dpi=150)
    plt.show()
    print(f"Grafiek opgeslagen als {out_name}")

if __name__ == '__main__':
    main()