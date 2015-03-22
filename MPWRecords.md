# Introduction #

Asm816 supports a subset of MPW records/templates. Records are similar to C `structs` or Pascal `records` or Merlin `dum` sections.

# Details #

```
;
; define the record/template
;
CloseRecGS RECORD 0
pCount ds.w 1
refNum ds.w 1
 ENDR

...

;
; this allocates the disk space and sets up the named equates.
;
close ds CloseRecGS

....

;
; can be accessed like normal
;

 lda open.refNum
 sta close.refNum
 _CloseGS close

```
