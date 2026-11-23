package com.example.vkedu.data.source

import com.example.vkedu.data.dto.AppDto
import com.example.vkedu.data.dto.CategoryDto

object AppsDataSource {
    val apps: List<AppDto> = listOf(
        AppDto(
            id = "fa2e31b8-1234-4cf7-9914-108a170a1b01",
            name = "QuickNote",
            description = "Быстрые заметки с синхронизацией между устройствами и тегами.",
            category = "UTILITIES",
            iconUrl = "https://fastly.picsum.photos/id/237/200/200.jpg?hmac=zHUGikXUDyLCCmvyww1izLK3R3k8oRYBRiTizZEdyfI",
        ),
        AppDto(
            id = "82ea6f63-2b2a-43e1-b54c-ec62e6e27229",
            name = "Calmalist",
            description = "Планировщик задач и привычек с мягкими напоминаниями и статистикой.",
            category = "PRODUCTIVITY",
            iconUrl = "https://fastly.picsum.photos/id/238/200/200.jpg?hmac=O4Jc6lqHVfaKVzLf8bWssNTbWzQoaRUC0TDXod9xDdM",
        ),
        AppDto(
            id = "c6f64035-c432-4aad-a550-a1dac2d3b047",
            name = "FitRiver",
            description = "Тренировки, шагомер и дневник активности в одном приложении.",
            category = "HEALTH",
            iconUrl = "https://fastly.picsum.photos/id/239/200/200.jpg?hmac=8JqlXUpZ9Xy0H6tMK8sCPQAYU9vUn9Qa8Kg-U9h3sCY",
        ),
        AppDto(
            id = "d22b7cda-2d36-4ebd-bfa3-be58770fa455",
            name = "TaskLoop",
            description = "Канбан-доски и таймер фокуса для команд и личных проектов.",
            category = "PRODUCTIVITY",
            iconUrl = "https://fastly.picsum.photos/id/240/200/200.jpg?hmac=PZiJibMUuv5Zpv9MAcqLaCYJOPt-XNuDh3RPBZiGcoY",
        ),
        AppDto(
            id = "f830e414-42a9-4fe6-af16-555a85f6f6c2",
            name = "Pixtrix",
            description = "Редактор фото с пресетами, слоями и быстрой ретушью.",
            category = "PHOTOGRAPHY",
            iconUrl = "https://fastly.picsum.photos/id/241/200/200.jpg?hmac=F-mDYyK1xUiXjlLd5PvE8EQpTwGfFndV7mMaANDtl28",
        ),
        AppDto(
            id = "ba31ce5d-b214-46e9-bf7c-35e99e70af14",
            name = "TinyChef",
            description = "Простые пошаговые рецепты и список покупок на неделю.",
            category = "FOOD",
            iconUrl = "https://fastly.picsum.photos/id/242/200/200.jpg?hmac=Z3aa8zbEQkEMFgnVh0Pn96vmCZHhJ17qzCrePYksrcY",
        ),
        AppDto(
            id = "204a939c-39fb-4c9d-bed9-daeeba8a465e",
            name = "GymMate",
            description = "Личный тренер: программы упражнений, подходы и прогресс.",
            category = "SPORTS",
            iconUrl = "https://fastly.picsum.photos/id/243/200/200.jpg?hmac=fW5ZwzzyTBy2t2MROp988oq12mZnKwN0coFLhZEE87s",
        ),
        AppDto(
            id = "1ec98719-1d86-4cdc-b99d-11c6c23d981e",
            name = "FlowCard",
            description = "Учёт личных финансов, бюджеты по категориям и отчёты.",
            category = "FINANCE",
            iconUrl = "https://fastly.picsum.photos/id/244/200/200.jpg?hmac=Q1gdvE6ZPZUX3nXkxvmzuc12eKVZ9XVEmSH3nCJ2OOo",
        ),
        AppDto(
            id = "d8180b87-56de-4737-b5ca-953d8c15ec80",
            name = "DreamSafe",
            description = "Дневник снов и медитации для спокойного вечера.",
            category = "HEALTH",
            iconUrl = "https://fastly.picsum.photos/id/2/200/200.jpg?hmac=isSWZUpv7D1D156XcADPOCZlfCG9mmvb8OlXFdvLdK0",
        ),
        AppDto(
            id = "572a8cc3-662e-47b8-9f69-b8f5454415c4",
            name = "MoodPin",
            description = "Отмечайте настроение и находите закономерности за месяц.",
            category = "SOCIAL",
            iconUrl = "https://fastly.picsum.photos/id/3/200/200.jpg?hmac=N5yYUNYl5gOUcaMmTtnNNtx839TN2qaNM4SaXhQl65U",
        ),
        AppDto(
            id = "152b90b4-4632-4acd-b8fd-7ae71bee95e7",
            name = "Shoply",
            description = "Списки покупок, сравнение цен и общие корзины для семьи.",
            category = "SHOPPING",
            iconUrl = "https://fastly.picsum.photos/id/247/200/200.jpg?hmac=oKt3N5MCdI8hCrzIbokjpVNzUuywbK64CJn1bfRAxbA",
        ),
        AppDto(
            id = "86e9355c-1ff0-4920-8cba-7bb2ea177625",
            name = "FeedJoy",
            description = "Новостная лента по интересам без лишнего шума.",
            category = "NEWS",
            iconUrl = "https://fastly.picsum.photos/id/248/200/200.jpg?hmac=36BllTJxy_tU762d2RYKfYaSQ3-RmP74hVxabGP_u3o",
        ),
        AppDto(
            id = "e571be45-3b91-4193-8fcd-ae7237ca3e97",
            name = "Scanwise",
            description = "Сканер документов с распознаванием текста и экспортом в PDF.",
            category = "BUSINESS",
            iconUrl = "https://fastly.picsum.photos/id/249/200/200.jpg?hmac=75zqoHvrxGGVnJnS8h0gUzZ3zniIk6PggG38GjmyOto",
        ),
        AppDto(
            id = "2b8e56f6-47e5-4a7c-94dc-5c282faf976d",
            name = "MemoSpark",
            description = "Читалка и коллекция цитат с умными подборками.",
            category = "BOOKS",
            iconUrl = "https://fastly.picsum.photos/id/250/200/200.jpg?hmac=23TaEG1txY5qYZ70amm2sUf0GYKo4v7yIbN9ooyqWzs",
        ),
    )

    val categories: List<CategoryDto> = listOf(
        CategoryDto("APP", "Приложения"),
        CategoryDto("GAME", "Игры"),
        CategoryDto("PRODUCTIVITY", "Производительность"),
        CategoryDto("SOCIAL", "Социальные сети"),
        CategoryDto("EDUCATION", "Образование"),
        CategoryDto("ENTERTAINMENT", "Развлечения"),
        CategoryDto("MUSIC", "Музыка"),
        CategoryDto("VIDEO", "Видео"),
        CategoryDto("PHOTOGRAPHY", "Фотография"),
        CategoryDto("HEALTH", "Здоровье"),
        CategoryDto("SPORTS", "Спорт"),
        CategoryDto("NEWS", "Новости"),
        CategoryDto("BOOKS", "Книги"),
        CategoryDto("BUSINESS", "Бизнес"),
        CategoryDto("FINANCE", "Финансы"),
        CategoryDto("TRAVEL", "Путешествия"),
        CategoryDto("MAPS", "Карты"),
        CategoryDto("FOOD", "Еда"),
        CategoryDto("SHOPPING", "Покупки"),
        CategoryDto("UTILITIES", "Утилиты"),
    )
}