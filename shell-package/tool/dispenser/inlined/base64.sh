#!/bin/sh

mdu_decode_base64() {
    __mdu_b64_b64="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    __mdu_b64_buffer=""
    __mdu_b64_padding=0
    
    # Lire et nettoyer l'entrée en une fois
    __mdu_b64_input=$(cat | tr -cd 'A-Za-z0-9+/=')
    
    # Traiter par blocs de 72 caractères (multiple de 4) pour réduire les appels
    __mdu_b64_len=${#__mdu_b64_input}
    __mdu_b64_pos=1
    __mdu_b64_chunk_size=72
    
    while [ $__mdu_b64_pos -le $__mdu_b64_len ]; do
        # Extraire un chunk
        __mdu_b64_chunk=""
        __mdu_b64_end=$((__mdu_b64_pos + __mdu_b64_chunk_size - 1))
        if [ $__mdu_b64_end -gt $__mdu_b64_len ]; then
            __mdu_b64_end=$__mdu_b64_len
        fi
        
        # Extraire le chunk sans boucle caractère par caractère
        __mdu_b64_chunk=$(printf '%s' "$__mdu_b64_input" | cut -c "$__mdu_b64_pos-$__mdu_b64_end")
        
        # Ajouter au buffer et traiter les groupes complets
        __mdu_b64_buffer="${__mdu_b64_buffer}${__mdu_b64_chunk}"
        
        # Traiter tous les groupes complets de 4 dans le buffer
        while [ ${#__mdu_b64_buffer} -ge 4 ]; do
            __mdu_b64_group=$(printf '%s' "$__mdu_b64_buffer" | cut -c 1-4)
            __mdu_b64_remaining=$(printf '%s' "$__mdu_b64_buffer" | cut -c 5-)
            
            # Compter le padding dans ce groupe
            __mdu_b64_group_padding=0
            case "$__mdu_b64_group" in
                *==) __mdu_b64_group_padding=2 ;;
                *=)  __mdu_b64_group_padding=1 ;;
            esac
            
            __mdu_b64_process_group "$__mdu_b64_group" "$__mdu_b64_group_padding"
            __mdu_b64_buffer="$__mdu_b64_remaining"
        done
        
        __mdu_b64_pos=$((__mdu_b64_end + 1))
    done
    
    # Traiter le buffer restant
    if [ -n "$__mdu_b64_buffer" ]; then
        while [ ${#__mdu_b64_buffer} -lt 4 ]; do
            __mdu_b64_buffer="${__mdu_b64_buffer}="
        done
        # Recompter le padding pour le dernier groupe
        __mdu_b64_group_padding=0
        case "$__mdu_b64_buffer" in
            *==) __mdu_b64_group_padding=2 ;;
            *=)  __mdu_b64_group_padding=1 ;;
        esac
        __mdu_b64_process_group "$__mdu_b64_buffer" "$__mdu_b64_group_padding"
    fi
    
    unset __mdu_b64_b64 __mdu_b64_buffer __mdu_b64_padding __mdu_b64_input __mdu_b64_len
    unset __mdu_b64_pos __mdu_b64_chunk_size __mdu_b64_chunk __mdu_b64_end __mdu_b64_group
    unset __mdu_b64_remaining __mdu_b64_group_padding
}

__mdu_b64_get_char_value() {
    __mdu_b64_c="$1"
    case "$__mdu_b64_c" in
        A) return 0;; B) return 1;; C) return 2;; D) return 3;; E) return 4;; F) return 5;; G) return 6;; H) return 7;;
        I) return 8;; J) return 9;; K) return 10;; L) return 11;; M) return 12;; N) return 13;; O) return 14;; P) return 15;;
        Q) return 16;; R) return 17;; S) return 18;; T) return 19;; U) return 20;; V) return 21;; W) return 22;; X) return 23;;
        Y) return 24;; Z) return 25;;
        a) return 26;; b) return 27;; c) return 28;; d) return 29;; e) return 30;; f) return 31;; g) return 32;; h) return 33;;
        i) return 34;; j) return 35;; k) return 36;; l) return 37;; m) return 38;; n) return 39;; o) return 40;; p) return 41;;
        q) return 42;; r) return 43;; s) return 44;; t) return 45;; u) return 46;; v) return 47;; w) return 48;; x) return 49;;
        y) return 50;; z) return 51;;
        0) return 52;; 1) return 53;; 2) return 54;; 3) return 55;; 4) return 56;; 5) return 57;; 6) return 58;; 7) return 59;;
        8) return 60;; 9) return 61;; +) return 62;; /) return 63;;
        *) return 0;;  # For '=' or any invalid character
    esac
}
    

__mdu_b64_process_group() {
    __mdu_b64_group="$1"
    __mdu_b64_pad_count="$2"
    
    # Optimisation : pre-compute values for each character
    __mdu_b64_char1=$(printf '%s' "$__mdu_b64_group" | cut -c 1)
    __mdu_b64_char2=$(printf '%s' "$__mdu_b64_group" | cut -c 2)
    __mdu_b64_char3=$(printf '%s' "$__mdu_b64_group" | cut -c 3)
    __mdu_b64_char4=$(printf '%s' "$__mdu_b64_group" | cut -c 4)
    
    # Get values
    __mdu_b64_get_char_value "$__mdu_b64_char1"; __mdu_b64_v1=$?
    __mdu_b64_get_char_value "$__mdu_b64_char2"; __mdu_b64_v2=$?
    __mdu_b64_get_char_value "$__mdu_b64_char3"; __mdu_b64_v3=$?
    __mdu_b64_get_char_value "$__mdu_b64_char4"; __mdu_b64_v4=$?
    
    # Build bytes
    __mdu_b64_o1=$(( (__mdu_b64_v1 << 2) | (__mdu_b64_v2 >> 4) ))
    __mdu_b64_o2=$(( ((__mdu_b64_v2 & 15) << 4) | (__mdu_b64_v3 >> 2) ))
    __mdu_b64_o3=$(( ((__mdu_b64_v3 & 3) << 6) | __mdu_b64_v4 ))
    
    # Write bytes
    case "$__mdu_b64_pad_count" in
        0)
            printf "\\$(printf '%03o' "$__mdu_b64_o1")"
            printf "\\$(printf '%03o' "$__mdu_b64_o2")"
            printf "\\$(printf '%03o' "$__mdu_b64_o3")"
            ;;
        1)
            printf "\\$(printf '%03o' "$__mdu_b64_o1")"
            printf "\\$(printf '%03o' "$__mdu_b64_o2")"
            ;;
        2)
            printf "\\$(printf '%03o' "$__mdu_b64_o1")"
            ;;
    esac
    
    unset __mdu_b64_group __mdu_b64_pad_count __mdu_b64_char1 __mdu_b64_char2 __mdu_b64_char3 __mdu_b64_char4
    unset __mdu_b64_v1 __mdu_b64_v2 __mdu_b64_v3 __mdu_b64_v4 __mdu_b64_o1 __mdu_b64_o2 __mdu_b64_o3
}

