Жигалова Елена Дмитриевна Б9124-09.03.03пикд3
API: "https://ghibliapi.vercel.app/"
API дает список фильмов с детальной информацией
Room таблица хранит: 
id
title
description
director
producer
releaseDate
rtScore
image
isFavorite
Сценарий: возможность добавление фильма в избранное, нажимая на "сердечко"

Юнит-тесты: 27
Интеграционные тесты: 10  
Нетривиальные тесты: 4  
Тесты с Flow: 4  

Юнит-тесты:
1. начальное состояние экрана Loading
2. успешная загрузка данных
3. ошибка загрузки
4. retry() после ошибки
5. поиск и фильтрация
6. добавление и удаление из избранного
7. сохранение избранного в кэше
8. обработка SavedStateHandle
9. пустой результат поиска
Интеграционные тесты:
1. запись и чтение из базы данных Room
2. обновление статуса избранного в БД
3. синхронизация API + Room
4. сохранение избранного после обновления
5. UI состояния (Loading, Error, Empty, Success)
Flow тесты :
Loading -> Empty -> Success
Нетривиальные тесты:
1. полная последовательность эмиссий
2. отсутствие дублирующихся эмиссий
3. поведение при новой подписке
4. пустой результат дает Empty, а не Success

<img width="281" height="557" alt="Снимок экрана 2026-05-03 171422" src="https://github.com/user-attachments/assets/61718932-678b-4c64-95fc-868a0e361b52" />
<img width="289" height="90" alt="Снимок экрана 2026-05-03 171401" src="https://github.com/user-attachments/assets/08015d74-243c-4a50-b3ec-ba5ecaf29b22" />
<img width="345" height="689" alt="Снимок экрана 2026-05-03 171355" src="https://github.com/user-attachments/assets/01504177-fc26-4ed4-a334-60f411deaae0" />
<img width="586" height="432" alt="Снимок экрана 2026-05-03 171542" src="https://github.com/user-attachments/assets/9612494b-8672-4e6e-8910-7ddcd524800e" />
