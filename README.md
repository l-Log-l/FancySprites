## FancySprites
Мод который дает создавать спрайты на контейнерах(инвентраь) на HUD и GUI проще говоря

![Пример спрайта меча воткнутого в инвеньтарь](https://cdn.modrinth.com/data/cached_images/f46f251ee2b138d00c5985fb73a88c13bdc0266a.png)

`fsprites/sword.mcmeta`
```json
{
  "enabled": true,
  "description": "Пример спрайта на тему Меч - Log :3",
  "id": "sword",
  "gui": ["minecraft:inventory"],
  "anchor": "top_right",
  "z_index": 1,
  "pos": [200, -35],
  "scale": 0.22
}
```
и в `fsprites/sword` картинка 0.png
![Вот пример меча](https://cdn.modrinth.com/data/cached_images/94f063435f237183fb3482d196dfd30ba0fbec5e.png)


## Количество параметров множество:
| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | bool | - | **Обезательно.** Для загрузки спрайта необходимо установить значение `true` (но в меню можно будет включить или выключить спрайт) |
| `id` | string | - | **Обезательно.** Айди но и как имя |
| `visible` | bool | `true` | Sprite visibility |
| `gui` | string или массив string | `minecraft:*` | Целевые GUIs; поддерживаются точные идентификаторы, подстановочные знаки и отрицание, например, `"!minecraft:game"` |
| `anchor` | string | `top_left` | Positioning anchor point |
| `pos` | [x, y] | [0, 0] | Position offset from anchor |
| `rot` | int | `0` | Rotation in degrees |
| `scale` | float | `1.0` | Scaling factor |
| `opacity` | float | `1.0` | Transparency (0.0-1.0) |
| `z_index` | int | `0` | Layer (positive=above, negative=below) |
| `blend` | string | `normal` | Blend mode |
| `fit` | string | `stretch` | Texture scaling mode |
| `width` | int/auto | `auto` | Sprite width |
| `height` | int/auto | `auto` | Sprite height |
| `origin` | [x, y] | [0, 0] | Rotation/scale center |
| `screen_space` | bool | `false` | Используйте экранные координаты вместо координат, относящихся к GUI |
| `animation` | object | - | Конфигурации анимации |

## Для `anchor`
```
top_left     top     top_right
  left      center     right
bottom_left  bottom  bottom_right
```

## Для `gui`
```
minecraft:inventory              minecraft:chest
minecraft:creative_inventory     minecraft:large_chest
minecraft:furnace                minecraft:crafting_table
minecraft:anvil                  minecraft:enchanting_table
minecraft:beacon                 minecraft:brewing_stand
minecraft:dispenser              minecraft:hopper
minecraft:shulker_box            minecraft:barrel
minecraft:villager               minecraft:horse
minecraft:game                   minecraft:* (all screens)
```

## Для `blend`
- `normal` - Стандартное альфа-смешивание графического интерфейса Minecraft
- Еще и `additive` `multiply` `screen`

## Animation Easing
- `none`, `linear`, `ease_in`, `ease_out`, `ease_in_out`
- `bounce`, `elastic`, `step`, `smooth`

В ESC в правом нижнем углу кнопка FancySprites для открытия меню мода
![Меню мода](https://cdn.modrinth.com/data/cached_images/7d731276fd4bec92008fb099269881e7d390758f.png)
здесь можно включать и выключать спрайты которые сделали


[Примеры rar архиве](https://drive.google.com/file/d/1JObe-lNSSAFJyUznDOufCEh08YXsWcin/view?usp=sharing)
переместите все в нутрь спрайты в `fsprites/`