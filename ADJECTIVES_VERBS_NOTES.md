# Прилагательные и глаголы: грамматика, методика, корпус

Рабочие заметки к расширению приложения (октябрь 2026). Здесь — что было собрано из открытых
источников, какие решения приняты и почему, и как пересобрать корпус.

## 1. Грамматика, которую тренирует приложение

### Прилагательные (přídavná jména)

| Тип | Образец | Признак | Что важно для учащегося |
|---|---|---|---|
| твёрдое | **mladý** | -ý / -á / -é | косвенные падежи -ého, -ému, -ém, -ým; вин. п. одуш. = род. п. (mladého); мн. ч. муж. одуш. **-í** со смягчением основы (mladí, hez**c**í, dra**z**í, češ**t**í) |
| мягкое | **jarní** | -í для всех родов | одна форма в им. п. для всех родов; косвенные -ího, -ímu, -ím; так же склоняются все сравнительные степени (lepší, starší, větší) |
| притяжательное | **otcův / matčin** | -ův от муж. владельца, -in от жен. | краткие «субстантивные» окончания: otcova, otcovu, otcově; в лок. ед. ч. вариативность otcově/otcovu |

Степени сравнения: -ejší/-ější (rychlejší, krásnější), -ší (mladší, starší), -čí с чередованием
(lehký → lehčí, drahý → dražší, tichý → tišší, vysoký → vyšší), супплетивные dobrý → lepší,
špatný → horší, velký → větší, malý → menší, dlouhý → delší. В корпусе поле `comparative`
хранится для всех градуируемых прилагательных (144 «реляционных» прилагательных вроде *policejní*
степеней не имеют — поле пустое).

Типичные ошибки учащихся (elon.io, Kulich 2019, Hrdlička 2014):
* класс прилагательного определяют «по смыслу», а не по окончанию (*letní* ≠ *teplý*);
* мягкие окончания у твёрдых прилагательных (*moderním* vs *modernem*);
* пропуск чередования в мн. ч. одуш. (*mladý muži* вместо *mladí muži*) и в сравнительной степени (*drahší* вместо *dražší*);
* смешение вин. п. одуш. и неодуш. (vidím mladého muže / vidím mladý dům);
* для русскоязычных: перенос русских окончаний (-ого/-ему), интерференция в тв. п. (-ым/-ou).

**Решение в UI:** прилагательное всегда тренируется **в согласовании с существительным**
(«mladý muž → mladého muže …»): форма существительного показана в ячейке серым как подсказка, игрок
расставляет формы прилагательного. Род меняется от раунда к раунду; набор существительных-партнёров
покрывает все основные образцы (pán, muž, hrad, stroj, žena, růže, píseň, kost, město, moře, kuře,
stavení) — см. `WordService.AGREEMENT_NOUNS`. Это ровно 14 ячеек, как у существительного, поэтому
счёт очков, таймер и режим «одна форма» работают без изменений.

### Глаголы (slovesa)

Классификация для учащихся — по 3-му лицу ед. ч. (так делают учебники Čeština expres / Česky krok
za krokem; академические 5 классов по Wikipedia «Czech conjugation» приведены к ним):

| Группа в приложении (`verbClass`) | Окончания | Императив | Примеры |
|---|---|---|---|
| **dělá** (-á) | -ám, -áš, -á, -áme, -áte, -ají | -ej: dělej! | hledat, čekat, znát, dávat |
| **prosí** (-í) | -ím, -íš, -í, -íme, -íte, -í / -ějí | без окончания: pros!, mluv!; после 2 согласных -i: mysli! | mluvit, vidět, myslet, umět |
| **kupuje** (-uje) | -uji/-uju, -uješ, -uje, -ujeme, -ujete, -ují/-ujou | -uj: kupuj! | pracovat, potřebovat, studovat |
| **tiskne** (-ne) | -nu, -neš, -ne, -neme, -nete, -nou | -ni: tiskni! | sednout si, začít, zapomenout |
| **nese** (-e) | -u, -eš, -e, -eme, -ete, -ou, часто с чередованием основы | čti!, piš!, peč! | číst, psát, pít, jet, brát |
| **nepravidelné** | — | — | být, mít, jít, chtít, vědět, jíst, moct (+ приставочные) |

Что тренируется на каждом глаголе (14 форм = одна таблица):
* настоящее время — 6 форм (já … oni);
* прошедшее время — l-причастие по роду/числу: on, ona, ono, oni, ony (5 форм; ср. р. мн. ч. *-a*
  опущен как редкий) — именно согласование *-li/-ly* и образование причастия (šel/šla, četl, tiskl)
  даёт больше всего ошибок;
* императив — ty, my, vy (3 формы; у модальных *muset, moct* ячейки пустые).

Дополнительно у каждого глагола записаны **вид** (nedokonavé/dokonavé/obouvidové) и **видовая
пара** (`pair`, есть у 396 из 581) — они показаны в шапке карточки, потому что учебники и
исследования (Leibniz-ZAS «The use of aspect in Czech L2») рекомендуют заучивать глаголы парами.
Будущее время не тренируется отдельно: у большинства глаголов несовершенного вида оно образуется
с *budu* + инфинитивом, у совершенного вида совпадает с формами настоящего. В корпусе отдельное
поле `future` заполнено у восьми глаголов, в том числе *být*, *jít → půjdu* и *jet → pojedu*.

Типичные ошибки (Wikipedia, elon.io, dspace.cuni.cz — работы о чешском русскоязычных):
* пропуск вспомогательного *jsem/jsi* в прошедшем времени (русская интерференция: «я делал» → *já dělal*);
* порядок и выбор возвратных *se/si*; поэтому в корпус включены 105 частотных возвратных глаголов
  (bát se, učit se, dívat se, sednout si, pamatovat si …) с частицей в каждой форме;
* выбор вида (*včera jsem dělal úkol* вместо *udělal*);
* -uju/-ujou, -ej vs -ejí: разговорные варианты помечены в подсказках справочника как hovorové;
* образование императива (kup!, vrať!, mysli!, pospěš si!).

## 2. Методика, учтённая в интерфейсе

* **Горизонтальный + вертикальный подход** (Hrdlička 2014): таблица целиком (full table) и
  форма за формой (single form) — оба режима теперь работают для всех трёх частей речи.
* **Образцы как справочный, а не основной инструмент**: в справочнике для каждого образца
  короткая подсказка-правило (tip_*), «другие слова этого типа», полная таблица.
* **Согласование вместо изолированных парадигм** прилагательных: каждая ячейка читается как
  словосочетание.
* **Частотность**: кандидаты в корпус ранжированы по списку cs_50k (OpenSubtitles 2018), что
  даёт лексику уровней A1–B1; вручную добавлены образцовые слова (prosit, tisknout, nést, otcův,
  matčin …), убраны нестандартные (-ej формы), вульгарные и ложные совпадения (pět, dít).
* **Повторение ошибок**: ключи списка ошибок получили префиксы `adj:` / `verb:`; режим «Review»
  обходит ошибки всех частей речи.

## 3. Корпус

| Файл | Записей | Источник форм |
|---|---|---|
| `database/src/main/assets/adjectives.jsonl` | 506 (283 твёрдых, 216 мягких, 7 притяжательных) | cs.wiktionary `{{Adjektivum (cs)}}` |
| `database/src/main/assets/verbs.jsonl` | 581 (476 обычных + 105 возвратных; 320 сов. / 257 несов. / 4 двувидовых) | cs.wiktionary `{{Sloveso (cs)}}`; 12 возвратных без страницы проспрягованы вручную |

Переводы: английский — en.wiktionary (глоссы), русский — cs.wiktionary → ru.wiktionary → ручной
словарь (`utils/corpus/manual_translations.py`, ≈ 400 слов) плюс ≈ 180 ручных исправлений там, где
Викисловарь давал не основное значение или не тот вид. Все записи вычитаны.

Пересборка: `python3 utils/corpus/build_corpus.py` (кэш страниц в `utils/corpus/cache/`, не в git;
скрипт докачивает недостающее через API Викисловаря). Лицензия исходных данных — CC BY-SA
(Wiktionary); список частот — CC BY-SA (hermitdave/FrequencyWords).

Формат записи глагола:
```json
{"wordId": 5, "word": "dělat", "aspect": "nedokonavé", "pair": "udělat", "verbClass": "dělá",
 "translation_ru": "делать", "translation_en": "to do; to make",
 "present": ["dělám","děláš","dělá","děláme","děláte","dělají"],
 "past": ["dělal","dělala","dělalo","dělali","dělaly"], "imperative": ["dělej","dělejme","dělejte"]}
```
Формат записи прилагательного: `cases[число][род][падеж]`, род в порядке m. živ., m. neživ., ž., s.

## 4. Квиз по словосочетаниям и дальнейшие идеи

Четвёртый пункт переключателя — **«Фразы»** (`PartOfSpeech.PHRASE`): прилагательное и существительное
склоняются целиком, в ячейке стоит слово‑подсказка падежа (*bez, ke, vidím, o, s*), а в режиме «одна
форма» вопрос звучит как «bez ___» с четырьмя вариантами‑словосочетаниями. Ключ в списке ошибок —
`phrase:<прилагательное> <существительное>`, заголовок показывается в согласованном виде (*milé město*).

Что ещё стоит сделать (по убыванию пользы):

1. **Контекстные предложения** вместо голых подсказок: «Jdu k ___ (mladý muž)», «Mluvíme o ___»,
   «Bydlím v ___». Нужен небольшой корпус шаблонов на каждый падеж (10–15 на падеж), остальное уже есть.
2. **Глагол + дополнение в нужном падеже**: *pomáhat* + дат., *bát se* + род., *věřit* + дат., *dívat se na* +
   вин. Поле `valence` в корпусе глаголов (из Викисловаря можно вытащить `{{Vazba|cs|…}}`), квиз «выбери
   форму существительного после глагола». Это самая частая ошибка русскоязычных по литературе.
3. **Фильтр групп** для глаголов и прилагательных в настройках (как сейчас фильтр по роду): тренировать
   только *‑í* группу или только мягкие прилагательные.
4. **Прошедшее время с вспомогательным глаголом** как отдельная секция (*dělal jsem, dělala jsi…*) —
   тренирует именно пропуск *jsem/jsi*, типичный для русскоязычных.
5. **Степени сравнения**: ячейки *mladý → mladší → nejmladší*; данные (`comparative`, `superlative`) уже в корпусе.
6. UI: в таблице глаголов на маленьких экранах 8 строк тесноваты — можно сворачивать императив в одну
   строку «ty! / my! / vy!» (3 ячейки) или дать таблице прокрутку с «прилипшими» заголовками секций.

## 5. Источники

* Wikipedia: [Czech declension](https://en.wikipedia.org/wiki/Czech_declension), [Czech conjugation](https://en.wikipedia.org/wiki/Czech_conjugation), [Rozkazovací způsob](https://cs.wikipedia.org/wiki/Rozkazovac%C3%AD_zp%C5%AFsob)
* elon.io Czech grammar: [hard vs soft adjectives](https://elon.io/grammar/czech/adjectives/declension/hard-vs-soft), [comparative formation](https://elon.io/grammar/czech/adjectives/comparison/comparative-formation), [aspect pairs](https://elon.io/grammar/czech/verbs/aspect/aspect-pairs-to-memorize)
* M. Hrdlička, [K prezentaci deklinace v češtině pro cizince](https://dl1.cuni.cz/pluginfile.php/1314703/mod_resource/content/1/Hrdlicka_K-prezentaci-deklinace_SALi-2014.pdf) (SALi 2014)
* K. Kulich, [Srovnání české a ruské deklinace podstatných a přídavných jmen se zaměřením na výuku češtiny pro cizince](https://dspace.cuni.cz/handle/20.500.11956/106509)
* [Prezentace časování sloves v přítomném čase v učebnicích češtiny pro cizince (pro rusky mluvící studenty)](https://dspace.cuni.cz/handle/20.500.11956/148043)
* [Specifika češtiny ruských studentů](https://dspace.cuni.cz/handle/20.500.11956/68928) (se/si, pomocné být, krátké tvary zájmen)
* [The use of aspect in Czech L2](https://zaspil.leibniz-zas.de/article/view/175) (ZAS Papers in Linguistics)
* [Referenční popis češtiny pro účely zkoušky z českého jazyka pro trvalý pobyt (A1–A2)](https://homepage.cestina-pro-cizince.cz/trvaly-pobyt/uploads/Dokumenty/referencni_popis_2014.pdf)
* Данные: [cs.wiktionary.org](https://cs.wiktionary.org), [en.wiktionary.org](https://en.wiktionary.org), [ru.wiktionary.org](https://ru.wiktionary.org), [hermitdave/FrequencyWords](https://github.com/hermitdave/FrequencyWords); MorfFlex CZ рассмотрен и отклонён из-за лицензии CC BY-NC-SA.
