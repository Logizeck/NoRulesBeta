# B0.3 TEST PLAN

1. Level Select: confirm 3 pages, 12 slots/page, navigation arrows, and correct level numbers.
2. Backgrounds: compare snowman (#21), selfie (#27), dojo-style levels, key/object rooms, and digital puzzles. They should visibly differ.
3. Simulated ad — level condition: complete 5 levels before 3 minutes; ad must appear after the fifth completion.
4. Simulated ad — timer condition: wait at least 3 minutes since the last ad/app start, then complete a level; ad must appear.
5. Ad close: close control must remain locked for ~3 s, then continue directly to the pending next level.
6. After an ad, confirm neither another five-level ad nor the three-minute ad fires prematurely: both counters restart from that ad.
7. Check menu/hint readability on all new backgrounds.
8. Re-test levels 21, 24, 27–35 for collisions or unreadable UI over the new scene themes.
