(ns com.platypub.themes.minimalist.config
  (:require [com.platypub.themes.default.config :refer [config]]))

(def fields
  {:com.platypub.themes.minimalist.site/newsletter-description
   {:label "Newsletter description"
    :default "Subscribe to my newsletter:"}})

(def site-fields
  [:com.platypub.site/description
   :com.platypub.site/image
   :com.platypub.site/redirects
   :com.platypub.site/primary-color
   :com.platypub.site/accent-color
   :com.platypub.site/author-name
   :com.platypub.site/author-url
   :com.platypub.site/author-image
   :com.platypub.site/embed-html
   :com.platypub.site/nav-links
   :com.platypub.themes.minimalist.site/newsletter-description])

(defn -main []
  (-> config
      ;; uncomment this to debug
      ;(assoc :render-site ["bb" "--debug" "run" "render-site"])
      (assoc :site-fields site-fields)
      (update :fields merge fields)
      (assoc-in [:fields :com.platypub.site/nav-links :default] "/ Home\n/archive/ Archive")
      prn))
