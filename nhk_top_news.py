"""NHK News Web トップニュース見出し取得スクリプト

毎朝実行して、トップニュースの見出しをテキストファイルに保存する。
保存先: news/YYYY-MM-DD.txt
"""

import xml.etree.ElementTree as ET
import requests
from datetime import datetime
from pathlib import Path

RSS_URL = "https://www3.nhk.or.jp/rss/news/cat0.xml"
SAVE_DIR = Path(__file__).parent / "news"


def fetch_top_news() -> list[dict]:
    """NHK RSSフィードからトップニュースの見出しを取得する。"""
    resp = requests.get(RSS_URL, timeout=15)
    resp.raise_for_status()

    root = ET.fromstring(resp.content)
    items = []
    for item in root.findall(".//item"):
        title = item.findtext("title", "").strip()
        link = item.findtext("link", "").strip()
        pub_date = item.findtext("pubDate", "").strip()
        if title:
            items.append({"title": title, "link": link, "date": pub_date})

    return items


def save_headlines(items: list[dict]) -> Path:
    """見出しをテキストファイルに保存する。"""
    SAVE_DIR.mkdir(exist_ok=True)
    today = datetime.now().strftime("%Y-%m-%d")
    filepath = SAVE_DIR / f"{today}.txt"

    with open(filepath, "w", encoding="utf-8") as f:
        f.write(f"NHK トップニュース - {today}\n")
        f.write("=" * 50 + "\n\n")
        for i, item in enumerate(items, 1):
            f.write(f"{i}. {item['title']}\n")
            f.write(f"   {item['link']}\n\n")
        f.write(f"取得時刻: {datetime.now().strftime('%H:%M:%S')}\n")
        f.write(f"取得件数: {len(items)} 件\n")

    return filepath


def main():
    print("NHK News Web からトップニュースを取得中...")
    items = fetch_top_news()

    if not items:
        print("見出しを取得できませんでした。")
        return

    filepath = save_headlines(items)
    print(f"取得件数: {len(items)} 件")
    print(f"保存先: {filepath}")
    print()
    for i, item in enumerate(items, 1):
        print(f"  {i}. {item['title']}")


if __name__ == "__main__":
    main()
