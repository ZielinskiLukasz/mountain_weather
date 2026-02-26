#!/bin/bash

FROM=40
TO=80

while [[ $# -gt 0 ]]; do
    case "$1" in
        --from) FROM="$2"; shift 2 ;;
        --to)   TO="$2";   shift 2 ;;
        *)
            echo "Usage: $0 [--from PREFIX] [--to PREFIX]"
            echo "  PREFIX is a 2-digit number (default: --from 40 --to 80)"
            exit 1
            ;;
    esac
done

for ((d12 = FROM; d12 <= TO; d12++)); do
    for ((d34 = 1; d34 <= 12; d34++)); do
        for ((d56 = 1; d56 <= 31; d56++)); do
            for ((d711 = 0; d711 <= 99999; d711++)); do
                printf "%02d%02d%02d%05d\n" "$d12" "$d34" "$d56" "$d711"
            done
        done
    done
done
