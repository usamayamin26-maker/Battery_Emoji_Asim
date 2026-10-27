package com.jhw.batteryemoji.ads

/**
 * Battery Emoji's side of the shared ads package (`ads/`, the same in every project): the Remote
 * Config parameter and its default JSON, handed to `AdsController.init` in [JHBatteryApplication].
 *
 * [DEFAULT_JSON] lists **every** ads key the app reads; debug builds log any key a screen reads that
 * is not here. Every flag is 0 and every unit id blank, so a build that never reaches Remote Config
 * shows no ads (debug builds swap in Google's test ids, see AdUnitIds).
 * Adding a screen or a placement = adding its keys here and to docs/BatteryEmojieAds.json.
 *
 * Key shapes: `<screen>_<top|bottom>_banner` / `_native` (slots; banner 0..5, native 0..5),
 * `is_show_*` (full-screen placements), `admob_<units>_banner_id` / `_native_id` (per-screen units,
 * blank = the shared `admob_app_*_id`), `counter_*` (how often each group of buttons shows an
 * interstitial).
 */
object AppAds {

    /** The Remote Config parameter holding the ads JSON (console value: docs/BatteryEmojieAds.json). */
    const val REMOTE_CONFIG_KEY = "BatteryEmojieAds"

    /**
     * Master switch. false = no ads anywhere: Remote Config is ignored (every flag reads 0), the
     * splash skips consent / SDK init and routes straight on, and no resume / welcome ad shows.
     */
    const val ADS_ENABLED = false

    const val DEFAULT_JSON = """
        {
          "is_show_welcome_screen": 0,
          "is_show_admob_resume_app_open": 0,
          "counter_bottom_nav": 0,
          "counter_screen_buttons": 0,
          "counter_back_btn": 0,
          "exit_bottom_banner": 0,
          "exit_bottom_native": 0,
          "splash_ad_preference": 0,
          "is_show_admob_splash_app_open": 0,
          "is_show_admob_splash_inter": 0,
          "is_show_admob_splash_native": 0,
          "splash_top_banner": 0,
          "splash_top_native": 0,
          "splash_bottom_banner": 0,
          "splash_bottom_native": 0,
          "is_show_admob_language_done_btn_inter": 0,
          "is_show_language_back_inter": 0,
          "is_show_admob_onboarding_skip_btn_inter": 0,
          "is_show_admob_onboarding_full_screen_native": 0,
          "is_show_admob_onboarding_get_started_inter": 0,
          "home_top_banner": 0,
          "home_top_native": 0,
          "home_bottom_banner": 0,
          "home_bottom_native": 0,
          "is_show_home_btn_inter": 0,
          "settings_top_banner": 0,
          "settings_top_native": 0,
          "settings_bottom_banner": 0,
          "settings_bottom_native": 0,
          "is_show_settings_btn_inter": 0,
          "search_top_banner": 0,
          "search_top_native": 0,
          "search_bottom_banner": 0,
          "search_bottom_native": 0,
          "is_show_search_btn_inter": 0,
          "is_show_search_back_inter": 0,
          "theme_top_banner": 0,
          "theme_top_native": 0,
          "theme_bottom_banner": 0,
          "theme_bottom_native": 0,
          "is_show_theme_btn_inter": 0,
          "is_show_theme_back_inter": 0,
          "animation_top_banner": 0,
          "animation_top_native": 0,
          "animation_bottom_banner": 0,
          "animation_bottom_native": 0,
          "is_show_animation_btn_inter": 0,
          "is_show_animation_back_inter": 0,
          "animation_see_all_top_banner": 0,
          "animation_see_all_top_native": 0,
          "animation_see_all_bottom_banner": 0,
          "animation_see_all_bottom_native": 0,
          "is_show_animation_see_all_btn_inter": 0,
          "is_show_animation_see_all_back_inter": 0,
          "custom_top_banner": 0,
          "custom_top_native": 0,
          "custom_bottom_banner": 0,
          "custom_bottom_native": 0,
          "is_show_custom_btn_inter": 0,
          "is_show_custom_back_inter": 0,
          "custom_detail_top_banner": 0,
          "custom_detail_top_native": 0,
          "custom_detail_bottom_banner": 0,
          "custom_detail_bottom_native": 0,
          "is_show_custom_detail_btn_inter": 0,
          "is_show_custom_detail_back_inter": 0,
          "sticker_top_banner": 0,
          "sticker_top_native": 0,
          "sticker_bottom_banner": 0,
          "sticker_bottom_native": 0,
          "is_show_sticker_btn_inter": 0,
          "is_show_sticker_back_inter": 0,
          "is_show_how_to_use_back_inter": 0,
          "native_ad_tag_text_color": "",
          "native_ad_tag_bg_color": "",
          "native_ad_tag_border_color": "",
          "native_heading_text_color": "",
          "native_description_text_color": "",
          "native_button_text_color": "",
          "native_button_bg_color": "",
          "native_ad_bg_color": "",
          "native_ad_border_color": "",
          "app_id": "",
          "admob_app_banner_id": "",
          "admob_app_native_id": "",
          "admob_app_inter_id": "",
          "admob_splash_app_open_id": "",
          "admob_splash_inter_id": "",
          "admob_splash_banner_id": "",
          "admob_splash_native_id": "",
          "admob_resume_app_open_id": "",
          "admob_language_done_btn_inter_id": "",
          "admob_onboarding_get_started_inter_id": "",
          "admob_onboarding_full_screen_native_id": "",
          "admob_exit_banner_id": "",
          "admob_exit_native_id": "",
          "admob_home_banner_id": "",
          "admob_home_native_id": "",
          "admob_settings_banner_id": "",
          "admob_settings_native_id": "",
          "admob_search_banner_id": "",
          "admob_search_native_id": "",
          "admob_theme_banner_id": "",
          "admob_theme_native_id": "",
          "admob_animation_banner_id": "",
          "admob_animation_native_id": "",
          "admob_animation_see_all_banner_id": "",
          "admob_animation_see_all_native_id": "",
          "admob_custom_banner_id": "",
          "admob_custom_native_id": "",
          "admob_custom_detail_banner_id": "",
          "admob_custom_detail_native_id": "",
          "admob_sticker_banner_id": "",
          "admob_sticker_native_id": ""
        }
    """
}
